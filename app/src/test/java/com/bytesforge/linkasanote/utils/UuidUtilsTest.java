/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bytesforge.linkasanote.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

//@RunWith(PowerMockRunner.class)
//@PrepareForTest(Base64.class)
public class UuidUtilsTest {

    /*@Before
    public void setupUuidUtils() {
        PowerMockito.mockStatic(Base64.class);
    }*/

    @Test
    public void checkConversionBetweenUuidAndKey() {
        String keyUUID = "00000000-0000-0000-0000-000000000000";
        byte[] bytesUUID = new byte[16];
        Arrays.fill(bytesUUID, (byte) 0x00);

        String keyBase64 = CommonUtils.charRepeat('A', 22);
        byte[] bytesBase64 = new byte[22];
        Arrays.fill(bytesBase64, (byte) 0x41);

        //when(Base64.encodeToString(bytesUUID, UuidUtils.FLAGS)).thenReturn(keyBase64);
        UUID uuid = UUID.fromString(keyUUID);
        String key = UuidUtils.keyFromUuid(uuid);
        assertThat(key, equalTo(keyBase64));

        //when(Base64.decode(keyBase64, UuidUtils.FLAGS)).thenReturn(bytesUUID);
        UUID uuidResult = UuidUtils.uuidFromKey(key);
        assertThat(uuidResult, equalTo(uuid));
    }
}
