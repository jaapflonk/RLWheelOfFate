package com.wheeloffate;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.Timer;

public class WheelComponent extends JComponent
{
	private static final int WHEEL_SIZE = 190;
	private static final int PADDING = 10;
	private static final int PREFERRED_SIZE = WHEEL_SIZE + PADDING * 2;
	private static final int ANIMATION_INTERVAL_MS = 16; // ~60fps
	private static final double SPIN_DURATION_MS = 4000.0;
	private static final double MIN_TOTAL_ROTATIONS = 5.0;
	private static final int POINTER_SIZE = 14;
	private static final Color[] SLICE_COLORS = {
		new Color(231, 76, 60),    // Red
		new Color(46, 204, 113),   // Green
		new Color(52, 152, 219),   // Blue
		new Color(241, 196, 15),   // Yellow
		new Color(155, 89, 182),   // Purple
		new Color(230, 126, 34),   // Orange
		new Color(26, 188, 156),   // Teal
		new Color(236, 112, 160),  // Pink
		new Color(52, 73, 94),     // Dark blue
		new Color(127, 140, 141),  // Gray
		new Color(192, 57, 43),    // Dark red
		new Color(39, 174, 96),    // Dark green
	};

	private static final Color BACKGROUND_COLOR = new Color(30, 30, 30);
	private static final Color BORDER_COLOR = new Color(200, 200, 200);
	private static final Color POINTER_COLOR = new Color(255, 255, 255);
	private static final Color TEXT_COLOR = Color.WHITE;
	private static final Color TEXT_SHADOW_COLOR = new Color(0, 0, 0, 160);
	private static final Color WINNER_GLOW_COLOR = new Color(255, 215, 0, 100);

	private List<WheelEntry> entries = new ArrayList<>();
	private double currentAngle = 0;
	private boolean spinning = false;
	private Timer animationTimer;
	private long spinStartTime;
	private double startAngle;
	private double targetAngle;
	private int winnerIndex = -1;
	private boolean showWinner = false;
	private int winnerFlashCount = 0;
	private Timer winnerFlashTimer;
	private Consumer<WheelEntry> onSpinComplete;

	public WheelComponent()
	{
		setPreferredSize(new Dimension(PREFERRED_SIZE, PREFERRED_SIZE));
		setMinimumSize(new Dimension(PREFERRED_SIZE, PREFERRED_SIZE));
		setBackground(BACKGROUND_COLOR);
		setOpaque(true);
	}

	public void setEntries(List<WheelEntry> entries)
	{
		this.entries = new ArrayList<>(entries);
		winnerIndex = -1;
		showWinner = false;
		repaint();
	}

	public void setOnSpinComplete(Consumer<WheelEntry> callback)
	{
		this.onSpinComplete = callback;
	}

	public boolean isSpinning()
	{
		return spinning;
	}

	public void spin(int winnerIdx)
	{
		if (spinning || entries.isEmpty())
		{
			return;
		}

		stopWinnerFlash();
		winnerIndex = -1;
		showWinner = false;
		spinning = true;

		// Always start from 0 so all clients see the same wheel
		currentAngle = 0;
		startAngle = 0;

		double sliceAngle = 360.0 / entries.size();

		double winnerMiddle = winnerIdx * sliceAngle + sliceAngle / 2.0;
		double desiredAngle = 90.0 - winnerMiddle;
		desiredAngle = ((desiredAngle % 360.0) + 360.0) % 360.0;

		double extraRotations = MIN_TOTAL_ROTATIONS + 3.0;
		targetAngle = desiredAngle + extraRotations * 360.0;

		spinStartTime = System.currentTimeMillis();

		animationTimer = new Timer(ANIMATION_INTERVAL_MS, e -> updateAnimation());
		animationTimer.start();
	}

	private void updateAnimation()
	{
		long elapsed = System.currentTimeMillis() - spinStartTime;
		double progress = Math.min(elapsed / SPIN_DURATION_MS, 1.0);

		double easedProgress = 1.0 - Math.pow(1.0 - progress, 3.0);

		currentAngle = startAngle + (targetAngle - startAngle) * easedProgress;

		if (progress >= 1.0)
		{
			spinning = false;
			animationTimer.stop();
			animationTimer = null;
			currentAngle = ((targetAngle % 360.0) + 360.0) % 360.0;

			winnerIndex = getSliceAtPointer();
			startWinnerFlash();

			if (onSpinComplete != null && winnerIndex >= 0 && winnerIndex < entries.size())
			{
				onSpinComplete.accept(entries.get(winnerIndex));
			}
		}

		repaint();
	}

	private int getSliceAtPointer()
	{
		if (entries.isEmpty())
		{
			return -1;
		}

		double sliceAngle = 360.0 / entries.size();
		double normalizedAngle = ((90.0 - currentAngle) % 360.0 + 360.0) % 360.0;
		return (int) (normalizedAngle / sliceAngle) % entries.size();
	}

	// === Winner Flash ===

	private void startWinnerFlash()
	{
		showWinner = true;
		winnerFlashCount = 0;
		winnerFlashTimer = new Timer(300, e ->
		{
			winnerFlashCount++;
			showWinner = !showWinner;
			if (winnerFlashCount >= 8)
			{
				showWinner = true;
				((Timer) e.getSource()).stop();
				winnerFlashTimer = null;
			}
			repaint();
		});
		winnerFlashTimer.start();
	}

	private void stopWinnerFlash()
	{
		if (winnerFlashTimer != null)
		{
			winnerFlashTimer.stop();
			winnerFlashTimer = null;
		}
	}

	// === Painting ===

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int width = getWidth();
		int height = getHeight();

		// Background
		g2d.setColor(BACKGROUND_COLOR);
		g2d.fillRect(0, 0, width, height);

		if (entries.isEmpty())
		{
			g2d.setColor(Color.GRAY);
			g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
			String msg = "Add entries to spin!";
			FontMetrics fm = g2d.getFontMetrics();
			g2d.drawString(msg, (width - fm.stringWidth(msg)) / 2, height / 2);
			g2d.dispose();
			return;
		}

		int wheelDiameter = Math.min(width, height) - PADDING * 2;
		int wheelX = (width - wheelDiameter) / 2;
		int wheelY = (height - wheelDiameter) / 2;
		int centerX = width / 2;
		int centerY = height / 2;
		double radius = wheelDiameter / 2.0;

		double sliceAngle = 360.0 / entries.size();

		// Draw slices
		for (int i = 0; i < entries.size(); i++)
		{
			double startDeg = currentAngle + i * sliceAngle;
			Color baseColor = SLICE_COLORS[i % SLICE_COLORS.length];

			if (winnerIndex == i && showWinner && !spinning)
			{
				baseColor = baseColor.brighter().brighter();
			}

			g2d.setColor(baseColor);
			g2d.fill(new Arc2D.Double(wheelX, wheelY, wheelDiameter, wheelDiameter,
				startDeg, sliceAngle, Arc2D.PIE));

			g2d.setColor(new Color(20, 20, 20));
			g2d.setStroke(new BasicStroke(1.5f));
			g2d.draw(new Arc2D.Double(wheelX, wheelY, wheelDiameter, wheelDiameter,
				startDeg, sliceAngle, Arc2D.PIE));
		}

		// Draw text on slices
		g2d.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, Math.min(12, (int) (radius / 10)))));
		FontMetrics fm = g2d.getFontMetrics();

		for (int i = 0; i < entries.size(); i++)
		{
			double midAngle = Math.toRadians(currentAngle + i * sliceAngle + sliceAngle / 2.0);
			double textRadius = radius * 0.62;
			int textX = (int) (centerX + textRadius * Math.cos(midAngle));
			int textY = (int) (centerY - textRadius * Math.sin(midAngle));

			String name = entries.get(i).getName();
			if (fm.stringWidth(name) > radius * 0.85)
			{
				while (name.length() > 3 && fm.stringWidth(name + "..") > radius * 0.85)
				{
					name = name.substring(0, name.length() - 1);
				}
				name = name + "..";
			}

			g2d.setColor(TEXT_SHADOW_COLOR);
			g2d.drawString(name, textX - fm.stringWidth(name) / 2 + 1, textY + fm.getAscent() / 2 + 1);
			g2d.setColor(TEXT_COLOR);
			g2d.drawString(name, textX - fm.stringWidth(name) / 2, textY + fm.getAscent() / 2);
		}

		// Outer ring
		g2d.setColor(BORDER_COLOR);
		g2d.setStroke(new BasicStroke(3f));
		g2d.drawOval(wheelX, wheelY, wheelDiameter, wheelDiameter);

		// Center circle
		int centerDotSize = 16;
		g2d.setColor(new Color(50, 50, 50));
		g2d.fillOval(centerX - centerDotSize / 2, centerY - centerDotSize / 2, centerDotSize, centerDotSize);
		g2d.setColor(BORDER_COLOR);
		g2d.setStroke(new BasicStroke(2f));
		g2d.drawOval(centerX - centerDotSize / 2, centerY - centerDotSize / 2, centerDotSize, centerDotSize);

		// Winner glow
		if (winnerIndex >= 0 && showWinner && !spinning)
		{
			g2d.setColor(WINNER_GLOW_COLOR);
			g2d.setStroke(new BasicStroke(5f));
			g2d.drawOval(wheelX - 3, wheelY - 3, wheelDiameter + 6, wheelDiameter + 6);
		}

		// Pointer
		drawPointer(g2d, centerX, wheelY - 2);

		g2d.dispose();
	}

	private void drawPointer(Graphics2D g2d, int x, int y)
	{
		Path2D pointer = new Path2D.Double();
		pointer.moveTo(x, y + POINTER_SIZE + 2);
		pointer.lineTo(x - POINTER_SIZE / 2.0, y - 2);
		pointer.lineTo(x + POINTER_SIZE / 2.0, y - 2);
		pointer.closePath();

		g2d.setColor(POINTER_COLOR);
		g2d.fill(pointer);
		g2d.setColor(new Color(40, 40, 40));
		g2d.setStroke(new BasicStroke(1.5f));
		g2d.draw(pointer);
	}

	public void reset()
	{
		if (animationTimer != null)
		{
			animationTimer.stop();
			animationTimer = null;
		}
		stopWinnerFlash();
		spinning = false;
		currentAngle = 0;
		winnerIndex = -1;
		showWinner = false;
		repaint();
	}

}
