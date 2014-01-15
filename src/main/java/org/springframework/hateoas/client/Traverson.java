/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.hateoas.client;

import static org.springframework.http.HttpMethod.*;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.LinkDiscoverers;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Rels.Rel;
import org.springframework.hateoas.hal.HalLinkDiscoverer;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * Component to ease traversing hypermedia APIs by following links with relation types. Highly inspired by the equally
 * named JavaScript library.
 * 
 * @see https://github.com/basti1302/traverson
 * @author Oliver Gierke
 */
public class Traverson {

	private final URI baseUri;
	private final RestTemplate template;
	private final LinkDiscoverers discoverers;
	private final List<MediaType> mediaTypes;

	/**
	 * Creates a new {@link Traverson} interacting with the given base URI and using the given {@link MediaType}s to
	 * interact with the service.
	 * 
	 * @param baseUri must not be {@literal null}.
	 * @param mediaType must not be {@literal null} or empty.
	 */
	public Traverson(URI baseUri, MediaType... mediaTypes) {

		Assert.notNull(baseUri, "Base URI must not be null!");
		Assert.notEmpty(mediaTypes, "At least one media must be given!");

		this.mediaTypes = Arrays.asList(mediaTypes);
		this.template = prepareTemplate(this.mediaTypes);

		this.baseUri = baseUri;

		LinkDiscoverer discoverer = new HalLinkDiscoverer();
		this.discoverers = new LinkDiscoverers(OrderAwarePluginRegistry.create(Arrays.asList(discoverer)));
	}

	private final RestTemplate prepareTemplate(List<MediaType> mediaTypes) {

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new StringHttpMessageConverter(Charset.forName("UTF-8")));

		if (mediaTypes.contains(MediaTypes.HAL_JSON)) {
			converters.add(getHalConverter());
		}

		RestTemplate template = new RestTemplate();
		template.setMessageConverters(converters);
		return template;
	}

	private final HttpMessageConverter<?> getHalConverter() {

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new Jackson2HalModule());

		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();

		converter.setObjectMapper(mapper);
		converter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));

		return converter;
	}

	public TraversalBuilder follow(String... rels) {
		return new TraversalBuilder().follow(rels);
	}

	private HttpEntity<?> prepareRequest(HttpHeaders headers) {

		HttpHeaders toSent = new HttpHeaders();
		toSent.putAll(headers);

		if (headers.getAccept().isEmpty()) {
			toSent.setAccept(mediaTypes);
		}

		return new HttpEntity<Void>(headers);
	}

	/**
	 * Builder API to customize traversals.
	 * 
	 * @author Oliver Gierke
	 */
	public class TraversalBuilder {

		private List<String> rels = new ArrayList<String>();
		private Map<String, Object> templateParameters = new HashMap<String, Object>();
		private HttpHeaders headers = new HttpHeaders();

		public TraversalBuilder follow(String... rels) {
			this.rels.addAll(Arrays.asList(rels));
			return this;
		}

		public TraversalBuilder withTemplateParameters(Map<String, Object> parameters) {
			this.templateParameters = parameters;
			return this;
		}

		public TraversalBuilder withHeaders(HttpHeaders headers) {
			this.headers = headers;
			return this;
		}

		public <T> T toObject(Class<T> type) {
			return template.getForObject(traverseToFinalUrl(), type, templateParameters);
		}

		public <T> T toObject(ParameterizedTypeReference<T> type) {
			HttpEntity<?> request = prepareRequest(headers);
			return template.exchange(traverseToFinalUrl(), GET, request, type, templateParameters).getBody();
		}

		public String toObject(String jsonPath) {

			String forObject = template.getForObject(traverseToFinalUrl(), String.class, templateParameters);
			return JsonPath.read(forObject, jsonPath).toString();
		}

		public <T> ResponseEntity<T> toEntity(Class<T> type) {
			return template.getForEntity(traverseToFinalUrl(), type, templateParameters);
		}

		private String traverseToFinalUrl() {
			return getAndFindLinkWithRel(baseUri.toString(), rels.iterator());
		}

		private String getAndFindLinkWithRel(String uri, Iterator<String> rels) {

			if (!rels.hasNext()) {
				return uri;
			}

			HttpEntity<?> request = prepareRequest(headers);

			ResponseEntity<String> responseEntity = template.exchange(uri, GET, request, String.class, templateParameters);
			MediaType contentType = responseEntity.getHeaders().getContentType();
			String responseBody = responseEntity.getBody();

			Rel rel = Rels.getRelFor(rels.next(), discoverers);
			Link link = rel.findInResponse(responseBody, contentType);

			if (link == null) {
				throw new IllegalStateException(String.format("Expected to find link with rel '%s' in response %s!", rel,
						responseBody));
			}

			return getAndFindLinkWithRel(link.getHref(), rels);
		}
	}
}
