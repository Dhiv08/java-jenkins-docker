package com.example.demo;

public class App {
  public static void main(String[] args) throws Exception {
    System.out.println("Hello from Java app running in Docker via Jenkins!");
    while (true) {
      Thread.sleep(60000);
    }
  }
}