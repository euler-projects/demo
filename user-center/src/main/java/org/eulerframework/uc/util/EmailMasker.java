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
 * Renders an email address in its display-only masked form, used for the
 * {@code email} extension surfaced by {@code GET /user/identities}.
 * <p>
 * Masking rules (per {@code Model-#-User-Identity.md}, e.g.
 * {@code user@example.com} → {@code u**r@e*****e.com}):
 * <ul>
 *     <li>Local part → keep the first and last character, replace the
 *         middle with asterisks; parts of one or two characters are fully
 *         masked.</li>
 *     <li>Domain → the label before the final dot is masked the same way;
 *         the top-level domain is kept as-is.</li>
 *     <li>Strings without {@code @} fall back to the same
 *         first-and-last-visible masking applied to the whole value.</li>
 * </ul>
 */
public final class EmailMasker {

    private EmailMasker() {
    }

    /**
     * Mask the given email address for display.
     *
     * @param email the raw email address; must be non-empty
     * @return the masked form, never {@code null}
     */
    public static String mask(String email) {
        Assert.hasText(email, "email must not be empty");
        int at = email.lastIndexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            // Not a well-formed address; mask the whole value.
            return maskPart(email);
        }
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);

        int lastDot = domain.lastIndexOf('.');
        String maskedDomain;
        if (lastDot > 0) {
            // Mask the label before the TLD, keep the TLD readable.
            maskedDomain = maskPart(domain.substring(0, lastDot)) + domain.substring(lastDot);
        } else {
            maskedDomain = maskPart(domain);
        }
        return maskPart(local) + "@" + maskedDomain;
    }

    /**
     * Keep the first and last character visible and replace the middle
     * with asterisks; values of one or two characters are fully masked.
     */
    private static String maskPart(String part) {
        int len = part.length();
        if (len <= 2) {
            return "*".repeat(len);
        }
        return part.charAt(0) + "*".repeat(len - 2) + part.charAt(len - 1);
    }
}
