package ph.chargemap.submission;

/**
 * Whether a submission proposes a brand-new station (NEW) or an edit to an existing,
 * already-published station (EDIT). Both flow through the same moderation queue.
 */
public enum SubmissionType {
    NEW,
    EDIT
}
