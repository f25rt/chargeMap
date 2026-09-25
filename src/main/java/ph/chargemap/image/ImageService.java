package ph.chargemap.image;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.NotFoundException;

import java.io.IOException;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Stores and serves uploaded images in MongoDB GridFS (design.md). Images are not
 * sensitive, so the read endpoint is public.
 */
@Service
public class ImageService {

    private static final long MAX_BYTES = 8L * 1024 * 1024; // 8 MB

    private final GridFsTemplate gridFsTemplate;

    public ImageService(GridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }

    /** Stores an uploaded image and returns its GridFS id. */
    public ObjectId store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Uploaded file must be an image");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Image exceeds the 8 MB limit");
        }
        try {
            return gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(), contentType);
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded image");
        }
    }

    public record LoadedImage(GridFsResource resource, String contentType) {
    }

    /** Loads an image resource by id for streaming. */
    public LoadedImage load(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException("Invalid image id: " + id);
        }
        GridFSFile file = gridFsTemplate.findOne(new Query(where("_id").is(new ObjectId(id))));
        if (file == null) {
            throw new NotFoundException("Image not found: " + id);
        }
        GridFsResource resource = gridFsTemplate.getResource(file);
        String contentType = resource.getContentType();
        return new LoadedImage(resource, contentType != null ? contentType : "application/octet-stream");
    }
}
