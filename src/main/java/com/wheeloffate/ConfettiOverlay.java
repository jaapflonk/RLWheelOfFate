package com.wheeloffate;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.Timer;

public class ConfettiOverlay extends JComponent
{
	private static final int CONFETTI_COUNT = 80;
	private static final double DURATION_MS = 3500.0;
	private static final int INTERVAL_MS = 16;

	private static final Color[] COLORS = {
		new Color(255, 215, 0),    // Gold
		new Color(231, 76, 60),    // Red
		new Color(46, 204, 113),   // Green
		new Color(52, 152, 219),   // Blue
		new Color(241, 196, 15),   // Yellow
		new Color(155, 89, 182),   // Purple
		new Color(230, 126, 34),   // Orange
		new Color(255, 255, 255),  // White
		new Color(236, 112, 160),  // Pink
		new Color(26, 188, 156),   // Teal
	};

	private final List<Particle> particles = new ArrayList<>();
	private Timer timer;
	private long startTime;

	public ConfettiOverlay()
	{
		setOpaque(false);
	}

	public void fire()
	{
		stop();
		particles.clear();

		int width = getWidth();
		int height = getHeight();
		if (width <= 0 || height <= 0)
		{
			return;
		}

		Random rand = new Random();

		for (int i = 0; i < CONFETTI_COUNT; i++)
		{
			// Spawn from random positions along the top
			double x = rand.nextDouble() * width;
			double y = -5 - rand.nextDouble() * 20;

			double vx = -2.0 + rand.nextDouble() * 4.0;
			double vy = 1.0 + rand.nextDouble() * 3.0;

			Color color = COLORS[rand.nextInt(COLORS.length)];
			int size = 4 + rand.nextInt(6);
			int shape = rand.nextInt(3);
			double rotation = rand.nextDouble() * 360;
			double rotSpeed = -10 + rand.nextDouble() * 20;
			// Stagger start so they don't all appear at once
			double delay = rand.nextDouble() * 800;

			particles.add(new Particle(x, y, vx, vy, color, size, shape, rotation, rotSpeed, delay));
		}

		// Also burst some from center
		int cx = width / 2;
		int cy = height / 3;
		for (int i = 0; i < CONFETTI_COUNT / 2; i++)
		{
			double angle = rand.nextDouble() * Math.PI * 2;
			double speed = 2.0 + rand.nextDouble() * 4.0;
			double vx = Math.cos(angle) * speed;
			double vy = Math.sin(angle) * speed - 2.0;

			Color color = COLORS[rand.nextInt(COLORS.length)];
			int size = 3 + rand.nextInt(6);
			int shape = rand.nextInt(3);
			double rotation = rand.nextDouble() * 360;
			double rotSpeed = -10 + rand.nextDouble() * 20;

			particles.add(new Particle(cx, cy, vx, vy, color, size, shape, rotation, rotSpeed, 0));
		}

		startTime = System.currentTimeMillis();
		timer = new Timer(INTERVAL_MS, e -> update());
		timer.start();
	}

	public void stop()
	{
		if (timer != null)
		{
			timer.stop();
			timer = null;
		}
		particles.clear();
		repaint();
	}

	private void update()
	{
		long elapsed = System.currentTimeMillis() - startTime;
		if (elapsed > DURATION_MS)
		{
			stop();
			return;
		}

		for (Particle p : particles)
		{
			if (elapsed < p.delay)
			{
				continue;
			}
			p.x += p.vx;
			p.y += p.vy;
			p.vy += 0.06; // gravity
			p.vx *= 0.995; // air drag
			p.rotation += p.rotSpeed;

			double life = (elapsed - p.delay) / (DURATION_MS - p.delay);
			// Fade out in the last 40%
			if (life > 0.6)
			{
				p.alpha = Math.max(0, 1.0 - (life - 0.6) / 0.4);
			}
			else
			{
				p.alpha = 1.0;
			}
		}

		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		if (particles.isEmpty())
		{
			return;
		}

		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		long elapsed = System.currentTimeMillis() - startTime;

		for (Particle p : particles)
		{
			if (elapsed < p.delay || p.alpha <= 0)
			{
				continue;
			}

			int alpha = (int) (p.alpha * 255);
			Color c = new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alpha);
			g2d.setColor(c);

			Graphics2D g2 = (Graphics2D) g2d.create();
			g2.translate(p.x, p.y);
			g2.rotate(Math.toRadians(p.rotation));

			switch (p.shape)
			{
				case 0: // Square
					g2.fillRect(-p.size / 2, -p.size / 2, p.size, p.size);
					break;
				case 1: // Circle
					g2.fillOval(-p.size / 2, -p.size / 2, p.size, p.size);
					break;
				case 2: // Ribbon
					g2.fillRect(-p.size, -p.size / 4, p.size * 2, p.size / 2);
					break;
			}

			g2.dispose();
		}

		g2d.dispose();
	}

	private static class Particle
	{
		double x, y, vx, vy;
		Color color;
		int size;
		int shape;
		double rotation;
		double rotSpeed;
		double delay;
		double alpha;

		Particle(double x, double y, double vx, double vy, Color color,
			int size, int shape, double rotation, double rotSpeed, double delay)
		{
			this.x = x;
			this.y = y;
			this.vx = vx;
			this.vy = vy;
			this.color = color;
			this.size = size;
			this.shape = shape;
			this.rotation = rotation;
			this.rotSpeed = rotSpeed;
			this.delay = delay;
			this.alpha = 1.0;
		}
	}
}
