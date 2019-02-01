package org.vadere.gui.components.utils;


import org.vadere.util.logging.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * The Resource class is for loading, changing, adding and manipulating properties in the
 * property file [applicationName]_config.properties. The global_config.properties file
 * plays a special role. If a property is not found in the specific file
 * e. g. postvisualization_config.properties than the global_config.properties will
 * be used as second step. Note: The global_config.properties are read only!
 *
 */
public class Resources {

	private static Logger logger = Logger.getLogger(Resources.class);

	private Properties properties = null;

	private String applicationName;

	private static Map<String, Resources> instanceMap = new HashMap<>();

	public static Resources getInstance(final String applicationName) {

		if (instanceMap.get(applicationName) == null) {
			instanceMap.put(applicationName, new Resources(applicationName));
		}
		return instanceMap.get(applicationName);
	}

	private Resources(final String applicationName) {
		this.applicationName = applicationName;
	}

	public String getProperty(final String key) {
		String prop = getProperties().getProperty(key);
		if (prop == null && !this.applicationName.equals("global")) {
			prop = Resources.getInstance("global").getProperty(key);
		}

		if (prop == null && this.applicationName.equals("global")) {
			logger.warn("property " + key + " was not found.");
		}
		return prop;
	}

	public boolean getBooleanProperty(final String key) {
		return Boolean.parseBoolean(getProperty(key));
	}

	public Object setProperty(final String key, final String value) {
		return getProperties().setProperty(key, value);
	}

	public Object removeProperty(final Object key) {
		return getProperties().remove(key);
	}

	public void putProperty(final Object key, final Object value) {
		getProperties().put(key, value);
	}

	private Properties getProperties() {
		if (properties == null) {
			properties = new Properties();
			InputStream in = null;
			try {
				in = Resources.class.getResourceAsStream("/config/" + applicationName + "_config.properties");
				properties.load(in);
			} catch (IOException e) {
				e.printStackTrace();
				logger.error("topographyError while loading properties for application: " + applicationName);
			} finally {
				try {
					in.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}

		}

		return properties;
	}

	public void save() throws IOException {
		if (properties != null) {
			BufferedOutputStream bout = null;
			try {
				URL location = Resources.class.getProtectionDomain().getCodeSource().getLocation();
				bout = new BufferedOutputStream(new FileOutputStream(location.getFile()
						+ "config/" + applicationName + "_config.properties"));
				properties.store(bout, "all the properties for the " + applicationName);
			} catch (IOException ex) {
				throw ex;
			} finally {
				if (bout != null) {
					bout.close();
				}
			}
		}
	}

	public Icon getIcon(final String name, final int iconWidth, final int iconHeight) {
		ImageIcon icon = new ImageIcon(Resources.class.getResource("/icons/" + name));
		Image img = icon.getImage().getScaledInstance(iconWidth, iconHeight, java.awt.Image.SCALE_AREA_AVERAGING);
		return new ImageIcon(img);
	}

	public Color getColor(final String name) {
		return stringToColor(getProperties().getProperty(name));
	}

	public BufferedImage getImage(final String name) {
		try {
			return ImageIO.read(Resources.class.getResource("/images/" + name));
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return null;
	}

	private static Color stringToColor(final String sColor) {
		return new Color(Integer.parseInt(sColor.substring(1, 3)), Integer.parseInt(sColor.substring(3, 5)),
				Integer.parseInt(sColor.substring(5, 7)));
	}
}
