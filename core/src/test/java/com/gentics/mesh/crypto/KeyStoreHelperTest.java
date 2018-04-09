package com.gentics.mesh.crypto;

import org.junit.Ignore;
import org.junit.Test;

public class KeyStoreHelperTest {

    @Ignore("The test assert nothing")
	@Test
	public void testKeyStore() throws Exception {
		KeyStoreHelper.gen("hmac_store.jceks", "password");
	}
}
