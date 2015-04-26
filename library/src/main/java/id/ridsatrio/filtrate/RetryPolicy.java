/*
 * Copyright (C) 2015 Ridho Hadi Satrio - All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package id.ridsatrio.filtrate;

/**
 * Policies that governs how {@link id.ridsatrio.filtrate.Filtrate} should retry upon the dismissal
 * of rating prompt.
 */
public enum RetryPolicy {
    /**
     * Will retry each time initial count has been triggered.
     * e.g. If initial is set to 3, it will be shown on the 3rd, 6th, 9th, ... times.
     */
    INCREMENTAL,

    /**
     * Will retry exponentially to be less intrusive.
     * e.g. If initial is set to 3, it will be shown on the 3rd, 6th, 12th, ... times.
     */
    EXPONENTIAL,

    /**
     * Will never retry.
     */
    NEVER_RETRY
}
