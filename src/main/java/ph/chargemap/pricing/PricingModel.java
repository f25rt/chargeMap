package ph.chargemap.pricing;

/** How a station charges for energy/time (product spec section 10). */
public enum PricingModel {
    PER_KWH,
    PER_MINUTE,
    FLAT,
    FREE
}
