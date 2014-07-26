package com.zaal.pigeon.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.zaal.pigeon.MeanPigeon;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "Drop";
        config.width = 800;
        config.height = 480;
        config.useGL30 = true;
        config.vSyncEnabled = true;
		new LwjglApplication(new MeanPigeon(), config);
	}
}
