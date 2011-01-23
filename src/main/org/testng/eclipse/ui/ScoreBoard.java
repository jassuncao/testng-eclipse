/**
 * 
 */
package org.testng.eclipse.ui;

import org.testng.ITestResult;

/**
 * @author jassuncao
 *
 */
public class ScoreBoard {
  
  protected int m_suitesTotalCount;
  protected volatile int m_suiteCount;
  
  protected int m_testsTotalCount;
  protected volatile int m_testCount;
  
  protected int m_methodTotalCount;      
  protected volatile int m_methodCount;
  
  protected volatile int m_passedCount;
  protected volatile int m_failedCount;
  protected volatile int m_skippedCount;
  protected volatile int m_successPercentageFailed;
  
  private long m_startTime;
  private long m_stopTime;
  
  public boolean hasErrors() {
    return m_failedCount > 0 || m_successPercentageFailed > 0;
  }

  public int getSkippedCount() {
    return m_skippedCount;
  }
  
  public int getStatus() {
    if (hasErrors()) return ITestResult.FAILURE;
    else if (m_skippedCount > 0) return ITestResult.SKIP;
    else return ITestResult.SUCCESS;
  }

  public long getElapsedTime() {
    return m_stopTime-m_startTime;
  }

  public int getFailedCount() {
    return m_failedCount;
  }

  public int getPassedCount() {
    return m_passedCount;
  }

  public void reset() {
    m_suitesTotalCount = 0;
    m_testsTotalCount = 0;
    m_methodTotalCount = 0;
    m_suiteCount = 0;
    m_testCount = 0;
    m_methodCount = 0;
    m_passedCount = 0;
    m_failedCount = 0;
    m_skippedCount = 0;
    m_successPercentageFailed = 0;
    m_startTime= 0L;
    m_stopTime= 0L;
  }

  public void setSuitesTotalCount(int suitesTotalsCount) {
    m_suitesTotalCount = suitesTotalsCount;  
  }

  public void setTestsTotalCount(int testsTotalCount) {
    m_testsTotalCount = testsTotalCount;
  }
  
  public int getTestsTotalCount() {
    return m_testsTotalCount;
  }

  public void startTimer() {
    m_startTime= System.currentTimeMillis();    
  }

  public void suiteFinished() {
    m_suiteCount++;
  }
  
  public void testFinished() {
    m_testCount++;
    if(m_testCount>m_methodTotalCount)
      m_methodTotalCount =m_testCount; 
  }

  public boolean isFinished() {
    return m_suitesTotalCount == m_suiteCount && m_suitesTotalCount>0;
  }

  public void stopTimer() {
    m_stopTime= System.currentTimeMillis();      
  }

  public void increaseMethodTotalCount(int testMethodCount) {
    m_methodTotalCount +=testMethodCount;    
  }

  public void testSuccess() {
    m_passedCount++;
    m_methodCount++;
  }

  public void testFailure() {
    m_failedCount++;
    m_methodCount++;
  }

  public void testSkipped() {
    m_skippedCount++;
    m_methodCount++;    
  }

  public void testPartialFailure() {
    m_successPercentageFailed++;
    m_methodCount++;
  }

  public int getTestCount() {
    return m_testCount;
  }

  public int getMethodsCount() {
    return m_methodCount;
  }

  public int getTotalMethodCount() {
    return m_methodTotalCount;
  }

  public boolean isStopped() {
    return m_suitesTotalCount == 0;
  }


}
