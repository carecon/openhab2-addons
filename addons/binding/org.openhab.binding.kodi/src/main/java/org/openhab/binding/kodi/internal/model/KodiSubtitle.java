/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.kodi.internal.model;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Class representing a Kodi audio stream (see https://kodi.wiki/view/JSON-RPC_API/v9#Player.Subtitle)
 *
 * @author m17design - Initial contribution
 */
@NonNullByDefault
public class KodiSubtitle {
    private int index;
    private @NonNullByDefault({}) String language;
    private @NonNullByDefault({}) String name;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}