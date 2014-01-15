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

import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkDiscoverers;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

/**
 * @author Oliver Gierke
 */
public class Rels {

	public static Rel getRelFor(String rel, LinkDiscoverers discoverers) {

		if (rel.startsWith("$")) {
			return new JsonPathRel(rel);
		}

		return new SimpleRel(rel, discoverers);
	}

	public interface Rel {

		Link findInResponse(String representation, MediaType mediaType);
	}

	static class SimpleRel implements Rel {

		private final String rel;
		private final LinkDiscoverers discoverers;

		private SimpleRel(String rel, LinkDiscoverers discoverers) {
			this.rel = rel;
			this.discoverers = discoverers;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.client.Rels.Rel#findInResponse(java.lang.String, org.springframework.http.MediaType)
		 */
		@Override
		public Link findInResponse(String response, MediaType mediaType) {
			return discoverers.getLinkDiscovererFor(mediaType).findLinkWithRel(rel, response);
		}
	}

	/**
	 * A relation that's being looked up by a JSONPath expression.
	 * 
	 * @author Oliver Gierke
	 */
	static class JsonPathRel implements Rel {

		private final String jsonPath;
		private final String rel;

		private JsonPathRel(String jsonPath) {

			this.jsonPath = jsonPath;

			String lastSegment = jsonPath.substring(jsonPath.lastIndexOf('.'));
			this.rel = lastSegment.contains("[") ? lastSegment.substring(0, lastSegment.indexOf("[")) : lastSegment;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.client.Rels.Rel#findInResponse(java.lang.String, org.springframework.http.MediaType)
		 */
		@Override
		public Link findInResponse(String representation, MediaType mediaType) {
			return new Link(JsonPath.<Object> read(representation, jsonPath).toString(), rel);
		}
	}
}
