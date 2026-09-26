package ph.chargemap.user;

/** Authorization roles (product spec section 37). */
public enum Role {
    USER,
    OPERATOR,
    ADMIN,
    /** Oversees all admins and branches; also satisfies ADMIN-guarded routes. */
    SUPER_ADMIN
}
