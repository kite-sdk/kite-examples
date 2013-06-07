package com.cloudera.cdk.examples.demo;

public class DebugJavaMain {
  public static void main(String[] args) {
    System.out.println("Debug Java Main");

    System.out.println("# Arguments: " + args.length);
    for (int i = 0; i < args.length; i++) {
      System.out.println("Argument[" + i + "]: " + args[i]);
    }
  }
}
