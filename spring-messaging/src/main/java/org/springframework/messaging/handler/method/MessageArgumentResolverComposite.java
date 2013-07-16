/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.handler.method;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Resolves method parameters by delegating to a list of registered
 * {@link MessageArgumentResolver}. Previously resolved method parameters are cached
 * for faster lookups.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageArgumentResolverComposite implements MessageArgumentResolver {

	protected final Log logger = LogFactory.getLog(getClass());

	private final List<MessageArgumentResolver> argumentResolvers =	new LinkedList<MessageArgumentResolver>();

	private final Map<MethodParameter, MessageArgumentResolver> argumentResolverCache =
			new ConcurrentHashMap<MethodParameter, MessageArgumentResolver>(256);


	/**
	 * Return a read-only list with the contained resolvers, or an empty list.
	 */
	public List<MessageArgumentResolver> getResolvers() {
		return Collections.unmodifiableList(this.argumentResolvers);
	}

	/**
	 * Whether the given {@linkplain MethodParameter method parameter} is supported by any registered
	 * {@link MessageArgumentResolver}.
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return getArgumentResolver(parameter) != null;
	}

	/**
	 * Iterate over registered {@link MessageArgumentResolver}s and invoke the one that supports it.
	 * @exception IllegalStateException if no suitable {@link MessageArgumentResolver} is found.
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {

		MessageArgumentResolver resolver = getArgumentResolver(parameter);
		Assert.notNull(resolver, "Unknown parameter type [" + parameter.getParameterType().getName() + "]");
		return resolver.resolveArgument(parameter, message);
	}

	/**
	 * Find a registered {@link MessageArgumentResolver} that supports the given method parameter.
	 */
	private MessageArgumentResolver getArgumentResolver(MethodParameter parameter) {
		MessageArgumentResolver result = this.argumentResolverCache.get(parameter);
		if (result == null) {
			for (MessageArgumentResolver resolver : this.argumentResolvers) {
				if (resolver.supportsParameter(parameter)) {
					result = resolver;
					this.argumentResolverCache.put(parameter, result);
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Add the given {@link MessageArgumentResolver}.
	 */
	public MessageArgumentResolverComposite addResolver(MessageArgumentResolver argumentResolver) {
		this.argumentResolvers.add(argumentResolver);
		return this;
	}

	/**
	 * Add the given {@link MessageArgumentResolver}s.
	 */
	public MessageArgumentResolverComposite addResolvers(List<? extends MessageArgumentResolver> argumentResolvers) {
		if (argumentResolvers != null) {
			for (MessageArgumentResolver resolver : argumentResolvers) {
				this.argumentResolvers.add(resolver);
			}
		}
		return this;
	}

}