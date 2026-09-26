package ph.chargemap.social;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.ForbiddenException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.image.ImageService;
import ph.chargemap.security.CurrentUser;
import ph.chargemap.social.SocialDtos.LikeStatusDto;
import ph.chargemap.social.SocialDtos.ReviewDto;
import ph.chargemap.station.Station;
import ph.chargemap.station.StationRepository;
import ph.chargemap.user.User;
import ph.chargemap.user.UserService;

import java.time.Instant;
import java.util.List;

/**
 * Station reviews (comment + optional photo, edit/delete own) and likes (toggle with a
 * running count). Like/unlike also appends a {@link StationLikeEvent} for the branch
 * admin's like-trend study.
 */
@Service
public class SocialService {

    private final StationReviewRepository reviewRepository;
    private final StationLikeRepository likeRepository;
    private final StationLikeEventRepository likeEventRepository;
    private final StationRepository stationRepository;
    private final ImageService imageService;
    private final UserService userService;

    public SocialService(StationReviewRepository reviewRepository,
                         StationLikeRepository likeRepository,
                         StationLikeEventRepository likeEventRepository,
                         StationRepository stationRepository, ImageService imageService,
                         UserService userService) {
        this.reviewRepository = reviewRepository;
        this.likeRepository = likeRepository;
        this.likeEventRepository = likeEventRepository;
        this.stationRepository = stationRepository;
        this.imageService = imageService;
        this.userService = userService;
    }

    // ----- Reviews -----

    /** Public list of a station's reviews, newest first. `mine` flags the caller's own. */
    public List<ReviewDto> listReviews(String stationId) {
        ObjectId sid = toId(stationId, "station");
        ObjectId me = currentUserIdOrNull();
        return reviewRepository.findByStationIdOrderByCreatedAtDesc(sid).stream()
                .map(r -> toDto(r, me))
                .toList();
    }

    /** Create a review (text + optional image) as the current active user. */
    public ReviewDto createReview(String stationId, String text, MultipartFile image) {
        ObjectId sid = toId(stationId, "station");
        if (!stationRepository.existsById(sid)) {
            throw new NotFoundException("Station not found: " + stationId);
        }
        User user = userService.requireActiveUser();
        if ((text == null || text.isBlank()) && (image == null || image.isEmpty())) {
            throw new BadRequestException("A comment or a photo is required");
        }
        Instant now = Instant.now();
        StationReview r = new StationReview();
        r.setStationId(sid);
        r.setUserId(user.getId());
        r.setAuthorName(user.getName());
        r.setText(text == null ? null : text.trim());
        if (image != null && !image.isEmpty()) {
            r.setImageId(imageService.store(image));
        }
        r.setCreatedAt(now);
        r.setUpdatedAt(now);
        return toDto(reviewRepository.save(r), user.getId());
    }

    /** Edit the caller's own review (text and/or replace image). */
    public ReviewDto editReview(String reviewId, String text, MultipartFile image) {
        User user = userService.requireActiveUser();
        StationReview r = loadReview(reviewId);
        if (!r.getUserId().equals(user.getId())) {
            throw new ForbiddenException("You can only edit your own review");
        }
        if (text != null) {
            r.setText(text.isBlank() ? null : text.trim());
        }
        if (image != null && !image.isEmpty()) {
            r.setImageId(imageService.store(image));
        }
        r.setUpdatedAt(Instant.now());
        return toDto(reviewRepository.save(r), user.getId());
    }

    /** Delete the caller's own review. */
    public void deleteReview(String reviewId) {
        User user = userService.requireCurrentUser();
        StationReview r = loadReview(reviewId);
        if (!r.getUserId().equals(user.getId())) {
            throw new ForbiddenException("You can only delete your own review");
        }
        reviewRepository.delete(r);
    }

    // ----- Likes -----

    public LikeStatusDto likeStatus(String stationId) {
        ObjectId sid = toId(stationId, "station");
        ObjectId me = currentUserIdOrNull();
        long count = likeRepository.countByStationId(sid);
        boolean liked = me != null && likeRepository.existsByStationIdAndUserId(sid, me);
        return new LikeStatusDto(count, liked);
    }

    /** Toggles the caller's like on a station and updates the denormalized count. */
    @Transactional
    public LikeStatusDto toggleLike(String stationId) {
        ObjectId sid = toId(stationId, "station");
        Station station = stationRepository.findById(sid)
                .orElseThrow(() -> new NotFoundException("Station not found: " + stationId));
        ObjectId userId = userService.requireActiveUser().getId();
        Instant now = Instant.now();

        var existing = likeRepository.findByStationIdAndUserId(sid, userId);
        int delta;
        boolean likedNow;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            delta = -1;
            likedNow = false;
        } else {
            likeRepository.save(new StationLike(sid, userId, now));
            delta = 1;
            likedNow = true;
        }

        long newCount = Math.max(0, station.getLikeCount() + delta);
        station.setLikeCount(newCount);
        stationRepository.save(station);
        likeEventRepository.save(new StationLikeEvent(sid, station.getBranchId(), delta, now));

        return new LikeStatusDto(newCount, likedNow);
    }

    // ----- helpers -----

    private ReviewDto toDto(StationReview r, ObjectId me) {
        return new ReviewDto(
                r.getId().toHexString(),
                r.getStationId().toHexString(),
                r.getUserId().toHexString(),
                r.getAuthorName(),
                r.getText(),
                r.getImageId() == null ? null : r.getImageId().toHexString(),
                me != null && me.equals(r.getUserId()),
                r.getCreatedAt(),
                r.getUpdatedAt());
    }

    private StationReview loadReview(String reviewId) {
        return reviewRepository.findById(toId(reviewId, "review"))
                .orElseThrow(() -> new NotFoundException("Review not found: " + reviewId));
    }

    private ObjectId toId(String id, String what) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException("Invalid " + what + " id: " + id);
        }
        return new ObjectId(id);
    }

    private ObjectId currentUserIdOrNull() {
        try {
            return CurrentUser.requireId();
        } catch (RuntimeException e) {
            return null; // anonymous viewer
        }
    }
}
