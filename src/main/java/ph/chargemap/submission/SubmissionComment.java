package ph.chargemap.submission;

import org.bson.types.ObjectId;

import java.time.Instant;

/** A comment in a submission's review thread (Requirement 2.4). */
public class SubmissionComment {

    private ObjectId authorId;
    private String authorName;
    private String role;
    private String text;
    private Instant createdAt;

    public SubmissionComment() {
    }

    public SubmissionComment(ObjectId authorId, String authorName, String role, String text,
                             Instant createdAt) {
        this.authorId = authorId;
        this.authorName = authorName;
        this.role = role;
        this.text = text;
        this.createdAt = createdAt;
    }

    public ObjectId getAuthorId() {
        return authorId;
    }

    public void setAuthorId(ObjectId authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
