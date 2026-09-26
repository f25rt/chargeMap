package ph.chargemap.social;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ph.chargemap.social.SocialDtos.LikeStatusDto;
import ph.chargemap.social.SocialDtos.ReviewDto;

import java.util.List;

/**
 * Station reviews + likes. Reads are public; writes require authentication (enforced by
 * SecurityConfig's {@code anyRequest().authenticated()} on non-GET). Review create/edit
 * are multipart to allow an optional photo.
 */
@RestController
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    // ----- Reviews -----
    @GetMapping("/api/stations/{stationId}/reviews")
    public List<ReviewDto> listReviews(@PathVariable String stationId) {
        return socialService.listReviews(stationId);
    }

    @PostMapping(value = "/api/stations/{stationId}/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto createReview(@PathVariable String stationId,
                                  @RequestParam(required = false) String text,
                                  @RequestParam(required = false) MultipartFile image) {
        return socialService.createReview(stationId, text, image);
    }

    @PutMapping(value = "/api/reviews/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReviewDto editReview(@PathVariable String reviewId,
                                @RequestParam(required = false) String text,
                                @RequestParam(required = false) MultipartFile image) {
        return socialService.editReview(reviewId, text, image);
    }

    @DeleteMapping("/api/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable String reviewId) {
        socialService.deleteReview(reviewId);
    }

    // ----- Likes -----
    @GetMapping("/api/stations/{stationId}/like")
    public LikeStatusDto likeStatus(@PathVariable String stationId) {
        return socialService.likeStatus(stationId);
    }

    @PostMapping("/api/stations/{stationId}/like")
    public LikeStatusDto toggleLike(@PathVariable String stationId) {
        return socialService.toggleLike(stationId);
    }
}
