package org.testng.eclipse.convert;


import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class JUnit3Test1 extends TestCase {
  private QueueTracker _queueTracker;

  public void setUp() {
    // should become @BeforeMethod
  }

  public void _test3Underscore() {
    fail("Should be a disabled test");
  }

  private void test3Private() {
    fail("Should be a disabled test");
  }

  public void testExecute() {
    assertEquals("CurrentEnqueueCountTotal is incorrect", 1,
        _queueTracker.getCurrentEnqueueCountTotal());
    assertEquals("CurrentQueueSize is incorrect", 1,
        _queueTracker.getCurrentQueueSize());
    assertTrue("run() should have been called", _queueTracker.isRun());
    assertEquals("CurrentQueueSize is incorrect after run()", 0,
        _queueTracker.getCurrentQueueSize());
  }

  public static Test suite() {
    return new TestSuite();
  }

  public void test1() {
		Assert.assertEquals(true, true);
		System.out.println("Worked");
	}

	public void test2() {
	  fail("Not implemented");
	}

}
