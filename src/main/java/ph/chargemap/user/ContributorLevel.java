package ph.chargemap.user;

/** Derives a contributor title from lifetime points (product spec reputation, section 9). */
public final class ContributorLevel {

    private ContributorLevel() {
    }

    public static String forPoints(long lifetimePoints) {
        if (lifetimePoints >= 2000) return "Guardian";
        if (lifetimePoints >= 500) return "Charger Scout";
        if (lifetimePoints >= 100) return "Scout";
        return "Newcomer";
    }
}
