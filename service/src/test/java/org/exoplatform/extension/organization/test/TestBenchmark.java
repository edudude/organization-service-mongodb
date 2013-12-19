package org.exoplatform.extension.organization.test;

import java.io.PrintWriter;

import junit.framework.TestCase;

import org.exoplatform.extension.organization.benchmark.UserHandlerBenshmark;

import com.google.caliper.runner.CaliperMain;

/**
 * 
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * 
 */
public class TestBenchmark extends TestCase {
  public void testBenchmark() {
    try {
      CaliperMain.exitlessMain(new String[] { "-imacro", UserHandlerBenshmark.class.getName() }, new PrintWriter(System.out), new PrintWriter(System.err));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}