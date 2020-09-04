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
 *     Preconditions.checkArgument(value >= 0.0, "negative value: %s", valu