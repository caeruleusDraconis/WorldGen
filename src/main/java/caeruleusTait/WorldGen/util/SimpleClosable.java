// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.util;

import java.io.Closeable;

public interface SimpleClosable extends Closeable {

    @Override
    void close();
}
