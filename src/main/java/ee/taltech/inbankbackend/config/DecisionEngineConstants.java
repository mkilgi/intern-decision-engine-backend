package ee.taltech.inbankbackend.config;

import java.time.Period;

/**
 * Holds all necessary constants for the decision engine.
 */
public class DecisionEngineConstants {
    public static final int MINIMUM_LOAN_AMOUNT = 2000;
    public static final int MAXIMUM_LOAN_AMOUNT = 10000;
    public static final int MAXIMUM_LOAN_PERIOD = 60;
    public static final int MINIMUM_LOAN_PERIOD = 12;
    public static final int SEGMENT_1_CREDIT_MODIFIER = 100;
    public static final int SEGMENT_2_CREDIT_MODIFIER = 300;
    public static final int SEGMENT_3_CREDIT_MODIFIER = 1000;

    public static final Period ESTONIA_LIFE_EXPECTANCY = Period.of(76, 7, 12);
    public static final Period LATVIA_LIFE_EXPECTANCY = Period.of(74, 3, 9);
    public static final Period LITHUANIA_LIFE_EXPECTANCY = Period.of(72, 5, 22);
}
