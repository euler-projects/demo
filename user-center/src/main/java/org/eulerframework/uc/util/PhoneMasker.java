/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eulerframework.uc.util;

import org.springframework.util.Assert;

/**
 * Renders an E.164 phone number in its display-only masked form, used for
 * the {@code phone} extension surfaced by {@code GET /user/identities}.
 * <p>
 * Masking rules (per {@code Model-#-User-Authentication-Factor.md}):
 * <ul>
 *     <li>Mainland-China numbers ({@code +86} + 11 digits) → keep the
 *         country code plus the first two digits and the last two digits,
 *         replace the middle seven digits with asterisks
 *         (e.g. {@code +8613812345678} → {@code +8613*******78}).</li>
 *     <li>Anything else → keep the first four characters and the last two,
 *         replace everything in between with asterisks. Numbers shorter
 *         than that fall back to a fully-masked form of equal length.</li>
 * </ul>
 */
public final class PhoneMasker {

    private static final String CN_PREFIX = "+86";

    private PhoneMasker() {
    }

    /**
     * Mask the given phone number for display.
     *
     * @param phone the raw phone number; must be non-empty
     * @return the masked form, never {@code null}
     */
    public static String mask(String phone) {
        Assert.hasText(phone, "phone must not be empty");
        if (phone.startsWith(CN_PREFIX) && phone.length() == 14) {
            // +86 + 2 visible + 7 stars + 2 visible = 14
            return phone.substring(0, 5) + "*******" + phone.substring(12);
        }
        int len = phone.length();
        if (len <= 6) {
            return "*".repeat(len);
        }
        int starCount = len - 6;
        return phone.substring(0, 4) + "*".repeat(starCount) + phone.substring(len - 2);
    }
}
