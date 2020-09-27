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
 * which are <i>not the caller's fault</i>. Doing so is <b>