/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.streaming.StreamingManager;

/**
 * Adapter interface which extends the {@link StreamingManager} contract with behavior
 * which should not be exposed in the public API.
 *
 * @since 4.0
 */
public interface StreamingManagerAdapter extends StreamingManager {


  /**
   * Register an {@link EventContext} so that the streaming manager can react to event context completion and clean up resources.
   *
   * @param eventContext event context to be registered
   */
  void registerEventContext(EventContext eventContext);

}
