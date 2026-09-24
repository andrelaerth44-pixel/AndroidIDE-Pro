/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  testImplementation(libs.tests.junit)
}
