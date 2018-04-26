package com.gentics.mesh.cli;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.etc.config.MeshOptions;

public class MeshImplTest {

	@Test
	public void testHostname() throws Exception {
		MeshImpl mesh = new MeshImpl(new MeshOptions());
		assertNotNull(mesh.getHostname());
	}
}
