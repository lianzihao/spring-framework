/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.server

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest
import org.springframework.web.testfixture.server.MockServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import kotlin.coroutines.CoroutineContext

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
class CoWebFilterTests {

	@Test
	fun filter() {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://example.com"))

		val chain = Mockito.mock(WebFilterChain::class.java)
		given(chain.filter(exchange)).willReturn(Mono.empty())

		val filter = MyCoWebFilter()
		val result = filter.filter(exchange, chain)

		StepVerifier.create(result)
			.verifyComplete()

		assertThat(exchange.attributes["foo"]).isEqualTo("bar")
	}

	@Test
	fun filterWithContext() {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://example.com"))

		val chain = Mockito.mock(WebFilterChain::class.java)
		given(chain.filter(exchange)).willReturn(Mono.empty())

		val filter = MyCoWebFilterWithContext()
		val result = filter.filter(exchange, chain)

		StepVerifier.create(result).verifyComplete()

		val context = exchange.attributes[CoWebFilter.COROUTINE_CONTEXT_ATTRIBUTE] as CoroutineContext
		assertThat(context).isNotNull()
		val coroutineName = context[CoroutineName.Key] as CoroutineName
		assertThat(coroutineName).isNotNull()
		assertThat(coroutineName.name).isEqualTo("foo")
	}

}


private class MyCoWebFilter : CoWebFilter() {
	override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
		exchange.attributes["foo"] = "bar"
		chain.filter(exchange)
	}
}

private class MyCoWebFilterWithContext : CoWebFilter() {
	override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
		withContext(CoroutineName("foo")) {
			chain.filter(exchange)
		}
	}
}
