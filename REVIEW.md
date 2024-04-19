# Code Review

## Overview
The project is well organized with all .java files managed in different packages. Important methods are provided with well explained javadocs and comments are generally concise and understandable. You made my job quite easy. Here are my findings:

## Main fixes

- Found a discrepancy of data type: **loanAmount** should always be int in every class and method.
- - This could lead to errors in data processing
```java
public class DecisionRequest {
    private String personalCode;
    private Long loanAmount; // Long -> int
    private int loanPeriod;
}
```
- Decision engine constants do not need the type **Integer**, primitive **int** works
```java
public static final Integer MINIMUM_LOAN_AMOUNT = 2000; // INTEGER -> int
```

## Other changes
- The decision engine is supposed to find a new loan period for requested loan amount.
- - When the requested amount is not approved with the requested loan period, the decision engine should respond with a loan that is higher or equal to the requested amount with a higher loan period if possible.
- - Also if the user requests for more than they can get, the decision engine should respond with the highest amount possible
- Changed some tests for the updated decision engine.
- Changed request url from ```localhost:8080/loan/decision``` to ```localhost:8080/loan``` for simplicity
- - Users are requesting loans.


## Conclusion
This project definetly well done and gives us a good foundation to work on. The decision engine changes are probably because of different perceptions of the task and I might as well be in the wrong. Thanks to your clean code and organizing it is very easy to change it to whatever it has to be. So good job  :D
