/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.testng.eclipse.ui;


import java.text.MessageFormat;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * A progress bar with a red/green indication for success or failure.
 */
public class ProgressBar extends Canvas {
  private static final int DEFAULT_WIDTH = 160;
  private static final int DEFAULT_HEIGHT = 16;
  
  private final Color m_oKColor;
  private final Color m_failureColor;
  private final Color m_stoppedColor;
  private final Color m_skippedColor;
  private final Color m_messageColor;
  
  private final ScoreBoard scoreBoard;

  public ProgressBar(Composite parent, ScoreBoard scoreBoard) {
    super(parent, SWT.NONE);
    this.scoreBoard = scoreBoard;

    addControlListener(new ControlAdapter() {
      public void controlResized(ControlEvent e) {
        redraw();
      }
    });
    addPaintListener(new PaintListener() {
      public void paintControl(PaintEvent e) {
        paint(e);
      }
    });
    addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
        m_failureColor.dispose();
        m_oKColor.dispose();
        m_stoppedColor.dispose();
        m_skippedColor.dispose();
      }
    });

    Display display = parent.getDisplay();
    m_failureColor = new Color(display, 159, 63, 63);
    m_oKColor = new Color(display, 95, 191, 95);
    m_skippedColor = new Color(display, 255, 193, 37);
    m_stoppedColor = new Color(display, 120, 120, 120);
    m_messageColor = display.getSystemColor(SWT.COLOR_BLACK);
  }

  private String getCurrentMessage() {
    String timeMessage = "";
    if(scoreBoard.isFinished()/* && scoreBoard.isStopped()*/){
      long elapsedTime = scoreBoard.getElapsedTime();
      timeMessage = "("+elapsedTime + " ms)";
    }
    return MessageFormat.format("Tests: {0}/{1} Methods: {2}/{3} {4}", new Object[]{
        scoreBoard.getTestCount(),
        scoreBoard.getTestsTotalCount(),
        scoreBoard.getMethodsCount(),
        scoreBoard.getTotalMethodCount(),
        timeMessage
    });   
  }

  private int calcBarWidth(Rectangle rect){
    float methodsTotalCount = scoreBoard.getTotalMethodCount();
    if(methodsTotalCount>0){
      /*
      //float progress;
      //float totalWork = methodsTotalCount;//*scoreBoard.getTestsTotalCount()/scoreBoard.getTestsTotalCount();
      progress = scoreBoard.getMethodsCount()/methodsTotalCount;
      /*
      if(methodsTotalCount>0){
        totalWork = methodsTotalCount*scoreBoard.getTestsTotalCount()/scoreBoard.getTestsTotalCount();
        progress = (scoreBoard.getMethodsCount()*scoreBoard.getTestCount())/totalWork;
      }
      else{
        totalWork = scoreBoard.getTestsTotalCount();
        progress = scoreBoard.getTestCount()/totalWork;
      } */  
      float progress = scoreBoard.getMethodsCount()/methodsTotalCount;
      return Math.round(progress*(rect.width - 2));
    }
    return 0;
  }

  private void setStatusColor(GC gc) {
    if(scoreBoard.hasErrors()){
      gc.setBackground(m_failureColor);
    }
    else if(scoreBoard.getSkippedCount()>0){
      gc.setBackground(m_skippedColor);
    }
    else if(scoreBoard.isStopped()){
      gc.setBackground(m_stoppedColor);
    }
    else {
      gc.setBackground(m_oKColor);
    }
  }

  private void drawBevelRect(GC gc, int x, int y, int w, int h, Color topleft, Color bottomright) {
    gc.setForeground(topleft);
    gc.drawLine(x, y, x + w - 1, y);
    gc.drawLine(x, y, x, y + h - 1);

    gc.setForeground(bottomright);
    gc.drawLine(x + w, y, x + w, y + h);
    gc.drawLine(x, y + h, x + w, y + h);
  }

  private void paint(PaintEvent event) {
    GC gc = event.gc;   
    drawProgress(gc);    
  }
  
  private void drawProgress(GC gc){
    Display disp = getDisplay();
    Rectangle rect = getClientArea();
    gc.fillRectangle(rect);
    drawBevelRect(gc,
                  rect.x,
                  rect.y,
                  rect.width - 1,
                  rect.height - 1,
                  disp.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW),
                  disp.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
    setStatusColor(gc);
    int w = Math.min(rect.width - 2, calcBarWidth(rect));
    gc.fillRectangle(1, 1, w, rect.height - 2);    
    String string = getCurrentMessage();
    gc.setFont(JFaceResources.getDefaultFont());
    FontMetrics fontMetrics = gc.getFontMetrics();
    int stringWidth = fontMetrics.getAverageCharWidth() * string.length();
    int stringHeight = fontMetrics.getHeight();
    gc.setForeground(m_messageColor);
    gc.drawString(string, (rect.width - stringWidth) / 2, (rect.height - stringHeight) / 2, true);
  }  
  public Point computeSize(int wHint, int hHint, boolean changed) {
    checkWidget();
    Point size = new Point(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    if (wHint != SWT.DEFAULT) {
      size.x = wHint;
    }
    if (hHint != SWT.DEFAULT) {
      size.y = hHint;
    }
    return size;
  }

  public void refreshProgress() {
    GC gc = new GC(this);
    drawProgress(gc);      
    gc.dispose();
  }
}
