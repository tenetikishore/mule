/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.param.UseConfig;

public abstract class AbstractImplicitExclusiveConfigTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {ImplicitExclusiveConfigExtension.class};
  }

  @Extension(name = "implicit")
  @Xml(namespace = "http://www.mulesoft.org/schema/mule/implicit", prefix = "implicit")
  @Configurations(value = {BlaConfig.class, BleConfig.class})
  public static class ImplicitExclusiveConfigExtension {
  }

  public static class BlaOperations {

    public void bla(@UseConfig BlaConfig bla) {}
  }

  public static class BleOperations {

    public void ble(@UseConfig BleConfig ble) {}
  }

  @Operations({BleOperations.class})
  @Configuration(name = "bleconf")
  public static class BleConfig {

  }

  @Operations({BlaOperations.class})
  @Configuration(name = "blaconf")
  public static class BlaConfig {

  }
}
