package com.coinomi.core;


/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * Static convenience methods that help a method or constructor check whether it was invoked
 * correctly (whether its <i>preconditions</i> have been met). These methods generally accept a
 * {@code boolean} expression which is expected to be {@code true} (or in the case of {@code
 * checkNotNull}, an object reference which is expected to be non-null). When {@code false} (or
 * {@code null}) is passed instead, the {@code Preconditions} method throws an unchecked exception,
 * which helps the calling method communicate to <i>its</i> caller that <i>that</i> caller has made
 * a mistake. Example: <pre>   {@code
 *
 *   /**
 *    * Returns the positive square root of the given value.
 *    *
 *    * @throws IllegalArgumentException if the value is negative
 *    *}{@code /
 *   public static double sqrt(double value) {
 *     Preconditions.checkArgument(value >= 0.0, "negative value: %s", value);
 *     // calculate the square root
 *   }
 *
 *   void exampleBadCaller() {
 *     double d = sqrt(-1.0);
 *   }}</pre>
 *
 * In this example, {@code checkArgument} throws an {@code IllegalArgumentException} to indicate
 * that {@code exampleBadCaller} made an error in <i>its</i> call to {@code sqrt}.
 *
 * <h3>Warning about performance</h3>
 *
 * <p>The goal of this class is to improve readability of code, but in some circumstances this may
 * come at a significant performance cost. Remember that parameter values for message construction
 * must all be computed eagerly, and autoboxing and varargs array creation may happen as well, even
 * when the precondition check then succeeds (as it should almost always do in production). In some
 * circumstances these wasted CPU cycles and allocations can add up to a real problem.
 * Performance-sensitive precondition checks can always be converted to the customary form:
 * <pre>   {@code
 *
 *   if (value < 0.0) {
 *     throw new IllegalArgumentException("negative value: " + value);
 *   }}</pre>
 *
 * <h3>Other types of preconditions</h3>
 *
 * <p>Not every type of precondition failure is supported by these methods. Continue to throw
 * standard JDK exceptions such as {@link java.util.NoSuchElementException} or {@link
 * UnsupportedOperationException} in the situations they are intended for.
 *
 * <h3>Non-preconditions</h3>
 *
 * <p>It is of course possible to use the methods of this class to check for invalid conditions
 * which are <i>not the caller's fault</i>. Doing so is <b>not recommended</b> because it is
 * misleading to future readers of the code and of stack traces. See
 * <a href="http://code.google.com/p/guava-libraries/wiki/ConditionalFailuresExplained">Conditional
 * failures explained</a> in the Guava User Guide for more advice.
 *
 * <h3>{@code java.util.Objects.requireNonNull()}</h3>
 *
 * <p>Projects which use {@code com.google.common} should generally avoid the use of {@link
 * java.util.Objects#requireNonNull(Object)}. Instead, use whichever of {@link
 * #checkNotNull(Object)} or {@link com.google.common.base.Verify#verifyNotNull(Object)} is appropriate to the situation.
 * (The same goes for the message-accepting overloads.)
 *
 * <h3>Only {@code %s} is supported</h3>
 *
 * <p>In {@code Preconditions} error message template strings, only the {@code "%s"} specifier is
 * supported, not the full range of {@link java.util.Formatter} specifiers. However, note that if
 * the number of arguments does not match the number of occurrences of {@code "%s"} in the format
 * string, {@code Preconditions} will still behave as expected, and will still include all argument
 * values in the error message; the message will simply not be formatted exactly as intended.
 *
 * <h3>More information</h3>
 *
 * <p>See the Guava User Guide on
 * <a href="http://code.google.com/p/guava-libraries/wiki/PreconditionsExplained">using {@code
 * Preconditions}</a>.
 *
 * @author Kevin Bourrillion
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible
public final class Preconditions {
    private Preconditions() {}

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression a boolean expression
     * @param errorMessage the exception message to use if the check fails; will be converted to a
     *     string using {@link String#valueOf(Object)}
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression, @Nullable Object errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression a boolean expression
     * @param errorMessageTemplate a template for the exception message should the check fail. The
     *     message is formed by replacing each {@code %s} placeholder in the template with an
     *     argument. These are 