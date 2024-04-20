package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.exception.PersonalCodeException;
import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeParser;
import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.*;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.Period;

/**
 * A service class that provides a method for calculating an approved loan amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
public class DecisionEngine {

    // Used to check for the validity of the presented ID code.
    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();
    private final EstonianPersonalCodeParser parser =  new EstonianPersonalCodeParser();
    private int creditModifier = 0;

    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 60 months (inclusive).
     * The loan amount must be between 2000 and 10000€ months (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     * @throws NoValidLoanException If there is no valid loan found for the given ID code, loan amount and loan period
     */
    public Decision calculateApprovedLoan(String personalCode, int loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException, InvalidAgeException {
        try {
            verifyInputs(personalCode, loanAmount, loanPeriod);
        } catch (Exception e) {
            return new Decision(null, null, e.getMessage());
        }

        int outputLoanAmount;
        creditModifier = getCreditModifier(personalCode);

        if (creditModifier == 0) {
            throw new NoValidLoanException("No valid loan found!");
        }

        while (highestValidLoanAmount(loanPeriod) < loanAmount) {
            loanPeriod+=6;
        }

        if (loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            outputLoanAmount = Math.min(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT, highestValidLoanAmount(loanPeriod));
        } else {
            // if there is no loan approved for the requested amount, then respond with the biggest possible loan
            loanPeriod = 60;
            outputLoanAmount = highestValidLoanAmount(loanPeriod);

            if (outputLoanAmount >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
                return new Decision(outputLoanAmount, loanPeriod, String.format("No valid loan found for amount %s eur", loanAmount));
            }

            throw new NoValidLoanException(String.format("No valid loan found for amount %s eur", loanAmount));
        }

        return new Decision(outputLoanAmount, loanPeriod, null);
    }

    /**
     * Calculates the largest valid loan for the current credit modifier and loan period.
     *
     * @return Largest valid loan amount
     */
    private int highestValidLoanAmount(int loanPeriod) {
        return creditModifier * loanPeriod;
    }

    /**
     * Calculates the credit modifier of the customer to according to the last four digits of their ID code.
     * Debt - 0000...2499
     * Segment 1 - 2500...4999
     * Segment 2 - 5000...7499
     * Segment 3 - 7500...9999
     *
     * @param personalCode ID code of the customer that made the request.
     * @return Segment to which the customer belongs.
     */
    private int getCreditModifier(String personalCode) {
        int segment = Integer.parseInt(personalCode.substring(personalCode.length() - 4));

        if (segment < 2500) {
            return 0;
        } else if (segment < 5000) {
            return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
        } else if (segment < 7500) {
            return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
        }

        return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
    }

    /**
     * Let's say that personal codes are in the same format in all countries, and they have the country data in them
     * This is completely random
     * @param personalCode given personal code
     * @return nationality
     */
    private String getCountry(String personalCode) {

        int digits = Integer.parseInt(personalCode.substring(7,10));

        if (digits < 200) {
            return "Estonia";
        } else if (digits < 500) {
            return "Latvia";
        } else {
            return "Lithuania";
        }
    }

    /**
     * get specific life expectancy for each country
     * @param country given country
     * @return life expectancy
     */
    private Period getLifeExpectancy(String country) {
        Period lifeExpectancy;
        switch (country) {
            case "Estonia" -> lifeExpectancy = lifeExpectancy = DecisionEngineConstants.ESTONIA_LIFE_EXPECTANCY;
            case "Latvia" -> lifeExpectancy = DecisionEngineConstants.LATVIA_LIFE_EXPECTANCY;
            case "Lithuania" -> lifeExpectancy = DecisionEngineConstants.LITHUANIA_LIFE_EXPECTANCY;
            default -> lifeExpectancy = Period.of(64, 10, 10);
        }

        return lifeExpectancy;
    }

    /**
     * Method for validating age restrictions
     * @param personalCode given personal ID code
     * @throws InvalidAgeException age is not valid to given restrictions
     * @throws PersonalCodeException unexpected server error
     */
    private void validateAge(String personalCode) throws InvalidAgeException, PersonalCodeException {

        // Verify age
        Period age = parser.getAge(personalCode);

        if (age.getYears() < 18) {
            throw new InvalidAgeException("You must be of legal age to apply for a loan!");
        }

        // Get personal data
        LocalDate dateOfBirth = parser.getDateOfBirth(personalCode);
        Period lifeExpectancy = getLifeExpectancy(getCountry(personalCode));

        //
        LocalDate expectedDateOfDeath = dateOfBirth.plus(lifeExpectancy);
        LocalDate dateNow = LocalDate.now();

        // Expected period alive must be more than 60 months (5 years)
        Period expectedPeriodAlive = Period.between(dateNow, expectedDateOfDeath);

        if (expectedPeriodAlive.getYears() < 5) {
            throw new InvalidAgeException("Loan application age limit exceeded!");
        }
    }

    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     */
    private void verifyInputs(String personalCode, int loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException, InvalidAgeException, PersonalCodeException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_AMOUNT <= loanAmount)
                || !(loanAmount <= DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT)) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_PERIOD <= loanPeriod)
                || !(loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD)) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }

        validateAge(personalCode);
    }
}
