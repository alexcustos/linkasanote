package com.bytesforge.linkasanote.utils;

import android.util.Base64;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Base64.class)
public class UuidUtilsTest {

    @Before
    public void setupUuidUtils() {
        PowerMockito.mockStatic(Base64.class);
    }

    @Test
    public void checkConversionBetweenUuidAndKey() {
        int flags = Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE;
        String keyUUID = "00000000-0000-0000-0000-000000000000";
        byte[] bytesUUID = new byte[16];
        Arrays.fill(bytesUUID, (byte) 0x00);

        String keyBase64 = CommonUtils.charRepeat('A', 22);
        byte[] bytesBase64 = new byte[22];
        Arrays.fill(bytesBase64, (byte) 0x41);

        when(Base64.encodeToString(bytesUUID, flags)).thenReturn(keyBase64);
        UUID uuid = UUID.fromString(keyUUID);
        String key = UuidUtils.keyFromUuid(uuid);
        assertThat(key, equalTo(keyBase64));

        when(Base64.decode(keyBase64, flags)).thenReturn(bytesUUID);
        UUID uuidResult = UuidUtils.uuidFromKey(key);
        assertThat(uuidResult, equalTo(uuid));
    }
}
