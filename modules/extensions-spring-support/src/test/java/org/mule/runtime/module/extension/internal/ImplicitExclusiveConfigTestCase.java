/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static java.util.Arrays.asList;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ImplicitExclusiveConfigTestCase extends AbstractImplicitExclusiveConfigTestCase {

  @Parameterized.Parameter
  public String configName;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{"implicit-exclusive-config.xml"}, {"multiple-implicit-exclusive-config.xml"},
        {"implicit-exclusive-config-with-declared-configs.xml"}});
  }

  @Override
  protected String getConfigFile() {
    return configName;
  }

  @Test
  public void getImplicitConfig() throws Exception {
    flowRunner("implicitConfig").run();
  }
}
