package ph.chargemap.moderation;

/** Body for an admin/operator direct edit of a station's descriptive details. */
public record StationDetailsUpdateRequest(String name, String operator, String address,
                                          String area) {
}
