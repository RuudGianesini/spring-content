package internal.org.springframework.content.rest.controllers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import internal.org.springframework.content.rest.support.LastModifiedDateTests;
import internal.org.springframework.content.rest.support.VersionHeaderTests;
import internal.org.springframework.content.rest.support.TestEntity3;
import internal.org.springframework.content.rest.support.TestEntity3ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity3Repository;
import internal.org.springframework.content.rest.support.TestEntity4;
import internal.org.springframework.content.rest.support.TestEntity4ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity4Repository;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.org.springframework.content.rest.support.StoreConfig;
import internal.org.springframework.content.rest.support.TestEntity;
import internal.org.springframework.content.rest.support.TestEntityContentRepository;
import internal.org.springframework.content.rest.support.TestEntityRepository;
import internal.org.springframework.content.rest.support.TestStore;

@RunWith(Ginkgo4jSpringRunner.class)
// @Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = { StoreConfig.class, DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class, RestConfiguration.class })
@Transactional
@ActiveProfiles("store")
public class ContentEntityRestControllerIntegrationTest {

	// different exported URI
	@Autowired
	TestEntityRepository repository;
	@Autowired
	TestEntityContentRepository contentRepository;

	// same exported URI
	@Autowired
	TestEntity3Repository repo3;
	@Autowired
	TestEntity3ContentRepository store3;

	// same exported URI
	@Autowired
	TestEntity4Repository repo4;
	@Autowired
	TestEntity4ContentRepository store4;

	@Autowired
	TestStore store;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity testEntity;
	private TestEntity3 testEntity3;
	private TestEntity4 testEntity4;

	private VersionHeaderTests versionHeaderTests;
	private LastModifiedDateTests lastModifiedDateTests;

	{
		Describe("ContentEntityRestController", () -> {
			BeforeEach(() -> {
				mvc = MockMvcBuilders.webAppContextSetup(context).build();
			});
			Context("given an entity is the subject of a repository and storage", () -> {
				Context("given the repository and storage are exported to the same URI", () -> {
					BeforeEach(() -> {
						testEntity3 = repo3.save(new TestEntity3());
						testEntity3.name = "test";
						testEntity3 = repo3.save(testEntity3);
					});
					Context("a GET to /{store}/{id} accepting hal+json", () -> {
						It("should return the entity", () -> {
							MockHttpServletResponse response = mvc
									.perform(get("/testEntity3s/" + testEntity3.id)
											.accept("application/hal+json"))
									.andExpect(status().isOk())
									.andReturn().getResponse();

							RepresentationFactory representationFactory = new StandardRepresentationFactory();
							ReadableRepresentation halResponse = representationFactory
									.readRepresentation("application/hal+json",
											new StringReader(response.getContentAsString()));
							assertThat(halResponse.getLinks().size(), is(2));
							assertThat(halResponse.getLinksByRel("testEntity3"), is(not(nullValue())));
						});
					});
					Context("a PUT to /{store}/{id} with a json body", () -> {
						It("should set Entities data and return 200", () -> {
							mvc.perform(put("/testEntity3s/" + testEntity3.id.toString())
									.content("{\"name\":\"Spring Content\"}")
									.contentType("application/hal+json"))
									.andExpect(status().is2xxSuccessful());

							Optional<TestEntity3> fetched = repo3.findById(testEntity3.id);
							assertThat(fetched.isPresent(), is(true));
							assertThat(fetched.get().name, is("Spring Content"));
							assertThat(fetched.get().contentId, is(nullValue()));
							assertThat(fetched.get().len, is(nullValue()));
							assertThat(fetched.get().mimeType, is(nullValue()));
						});
					});
					Context("a PATCH to /{store}/{id} with a json body", () -> {
						It("should patch the entity data and return 200", () -> {
							mvc.perform(patch("/testEntity3s/" + testEntity3.id.toString())
									.content("{\"name\":\"Spring Content Modified\"}")
									.contentType("application/hal+json"))
									.andExpect(status().is2xxSuccessful());

							Optional<TestEntity3> fetched = repo3.findById(testEntity3.id);
							assertThat(fetched.isPresent(), is(true));
							assertThat(fetched.get().name, is("Spring Content Modified"));
							assertThat(fetched.get().contentId, is(nullValue()));
							assertThat(fetched.get().len, is(nullValue()));
							assertThat(fetched.get().mimeType, is(nullValue()));
						});
					});
					Context("a HEAD to /{store}/{id} with a json body", () -> {
						It("should patch the entity data and return 200", () -> {
							mvc.perform(head("/testEntity3s/" + testEntity3.id.toString()))
									.andExpect(status().is2xxSuccessful());
						});
					});
				});
				Context("given the repository and storage are exported to different URIs", () -> {
					BeforeEach(() -> {
						testEntity = repository.save(new TestEntity());
					});
					Context("an OPTIONS request from a known host", () -> {
						It("should return the relevant cors headers and OK", () -> {
							mvc.perform(options("/testEntitiesContent/" + testEntity.id)
									.header("Access-Control-Request-Method", "POST")
									.header("Origin", "http://www.someurl.com"))
									.andExpect(status().isOk())
									.andExpect(header().string("Access-Control-Allow-Origin",
											"http://www.someurl.com"));
						});
					});
					Context("an OPTIONS request from an unknown host", () -> {
						It("should be forbidden", () -> {
							mvc.perform(options("/testEntitiesContent/" + testEntity.id)
									.header("Access-Control-Request-Method", "POST")
									.header("Origin", "http://www.someotherurl.com"))
									.andExpect(status().isForbidden());
						});
					});
					Context("a GET to /{store}/{id} accepting a content mime-type", () -> {
						It("should return 404", () -> {
							mvc.perform(get("/testEntitiesContent/" + testEntity.id)
									.accept("text/plain")).andExpect(status().isNotFound());
						});
					});
					Context("a PUT to /{store}/{id} with a content body", () -> {
						It("should set the content and return 201", () -> {
							mvc.perform(
									put("/testEntitiesContent/" + testEntity.id.toString())
											.content("Hello New Spring Content World!")
											.contentType("text/plain"))
									.andExpect(status().isCreated());

							Optional<TestEntity> fetched = repository.findById(testEntity.id);
							assertThat(fetched.isPresent(), is(true));
							assertThat(fetched.get().contentId, is(not(nullValue())));
							assertThat(fetched.get().len, is(31L));
							assertThat(fetched.get().mimeType, is("text/plain"));
							assertThat(
									IOUtils.toString(
											contentRepository.getContent(fetched.get())),
									is("Hello New Spring Content World!"));
						});
					});
					Context("a DELETE to /{store}/{id} with a mime-type", () -> {
						It("should return 404", () -> {
							mvc.perform(delete("/testEntitiesContent/" + testEntity.id)
									.accept("text/plain")).andExpect(status().isNotFound());
						});
					});
					Context("a POST to /{store}/{id} with a multi-part form-data request", () -> {
						It("should set the content and return 200", () -> {
							String content = "This is Spring Content!";

							mvc.perform(multipart("/testEntitiesContent/" + testEntity.getId())
									.file(new MockMultipartFile("file", "test-file.txt", "text/plain", content.getBytes())))
									.andExpect(status().isCreated());

							Optional<TestEntity> fetched = repository.findById(testEntity.id);
							assertThat(fetched.isPresent(), is(true));
							assertThat(fetched.get().contentId, is(not(nullValue())));
							assertThat(fetched.get().originalFileName, is("test-file.txt"));
							assertThat(fetched.get().mimeType, is("text/plain"));
							assertThat(fetched.get().len, is(new Long(content.length())));
						});
					});

					Context("given the Entity has content", () -> {
						BeforeEach(() -> {
							contentRepository.setContent(testEntity, new ByteArrayInputStream(
									"Hello Spring Content World!".getBytes()));
							testEntity.mimeType = "text/plain";
							testEntity = repository.save(testEntity);
						});
						Context("a GET to /{store}/{id}", () -> {
							It("should return the original content and 200", () -> {
								MockHttpServletResponse response = mvc
										.perform(get("/testEntitiesContent/" + testEntity.id)
												.accept("text/plain"))
										.andExpect(status().isOk()).andReturn().getResponse();

								assertThat(response, is(not(nullValue())));
								assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
							});
						});
						Context("a GET to /{store}/{id} with no accept header", () -> {
							It("should return the original content", () -> {
								MockHttpServletResponse response = mvc
										.perform(get("/testEntitiesContent/" + testEntity.id)
												/* .accept("text/plain") */).andExpect(status().isOk())
										.andReturn().getResponse();

								assertThat(response, is(not(nullValue())));
								assertThat(response.getContentAsString(),
										is("Hello Spring Content World!"));
							});
						});
						Context("a GET to /{store}/{id} with a mime type that matches a renderer", () -> {
							It("should return the rendition and 200", () -> {
								MockHttpServletResponse response = mvc
										.perform(get("/testEntitiesContent/" + testEntity.id)
												.accept("text/html"))
										.andExpect(status().isOk()).andReturn()
										.getResponse();

								assertThat(response, is(not(nullValue())));
								assertThat(response.getContentAsString(), is("<html><body>Hello Spring Content World!</body></html>"));
							});
						});
						Context("a GET to /{store}/{id} with multiple mime types the last of which matches the content", () -> {
							It("should return the original content and 200", () -> {
								MockHttpServletResponse response = mvc.perform(
										get("/testEntitiesContent/" + testEntity.id)
												.accept(new String[] { "text/xml",
														"text/*" }))
										.andExpect(status().isOk()).andReturn()
										.getResponse();

								assertThat(response, is(not(nullValue())));
								assertThat(response.getContentAsString(),
										is("Hello Spring Content World!"));
							});
						});
						Context("a GET to /{store}/{id} with a range header", () -> {
							It("should return the content range and 206", () -> {
								MockHttpServletResponse response = mvc
										.perform(get("/testEntitiesContent/" + testEntity.id)
												.accept("text/plain")
												.header("range", "bytes=6-19"))
										.andExpect(status().isPartialContent()).andReturn()
										.getResponse();

								assertThat(response, is(not(nullValue())));
								assertThat(response.getContentAsString(),
										is("Spring Content"));
							});
						});
						Context("a PUT to /{store}/{id}", () -> {
							It("should overwrite the content and return 200", () -> {
								mvc.perform(put("/testEntitiesContent/" + testEntity.id)
										.content("Hello Modified Spring Content World!")
										.contentType("text/plain"))
										.andExpect(status().isOk());

								assertThat(
										IOUtils.toString(
												contentRepository.getContent(testEntity)),
										is("Hello Modified Spring Content World!"));
							});
						});
						Context("a POST to /{store}/{id} with a multi-part request", () -> {
							It("should overwrite the content and return 200", () -> {

								String content = "This is Modified Spring Content!";

								mvc.perform(multipart(
										"/testEntitiesContent/" + testEntity.id.toString())
										.file(new MockMultipartFile("file",
												"test-file-modified.txt",
												"text/plain", content.getBytes())))
										.andExpect(status().isOk());

								Optional<TestEntity> fetched = repository
										.findById(testEntity.id);
								assertThat(fetched.isPresent(), is(true));
								assertThat(fetched.get().contentId, is(not(nullValue())));
								assertThat(fetched.get().originalFileName,
										is("test-file-modified.txt"));
								assertThat(fetched.get().mimeType, is("text/plain"));
								assertThat(fetched.get().len, is(new Long(content.length())));
							});
						});
						Context("a DELETE to /{store}/{id} with the mimetype", () -> {
							It("should delete the content and return a 200 response", () -> {
								mvc.perform(delete("/testEntitiesContent/" + testEntity.id)
										.contentType("text/plain"))
										.andExpect(status().isNoContent());

								assertThat(contentRepository.getContent(testEntity),
										is(nullValue()));
							});
						});
					});
				});
				Context("given an entity with @Version", () -> {
					BeforeEach(() -> {
						testEntity4 = new TestEntity4();
						store4.setContent(testEntity4, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
						testEntity4.mimeType = "text/plain";
						testEntity4 = repo4.save(testEntity4);
						String url = "/testEntity4s/" + testEntity4.getId();

						versionHeaderTests.setMvc(mvc);
						versionHeaderTests.setUrl(url);
						versionHeaderTests.setRepo(repo4);
						versionHeaderTests.setStore(store4);
						versionHeaderTests.setEtag(format("\"%s\"", testEntity4.getVersion()));
					});
					versionHeaderTests = new VersionHeaderTests();
				});

				Context("given an entity with @LastModifiedDate", () -> {
					BeforeEach(() -> {
						String content = "Hello Spring Content LastModifiedDate World!";

						testEntity4 = new TestEntity4();
						store4.setContent(testEntity4, new ByteArrayInputStream(content.getBytes()));
						testEntity4.mimeType = "text/plain";
						testEntity4 = repo4.save(testEntity4);
						String url = "/testEntity4s/" + testEntity4.getId();

						lastModifiedDateTests.setMvc(mvc);
						lastModifiedDateTests.setUrl(url);
						lastModifiedDateTests.setLastModifiedDate(testEntity4.getModifiedDate());
						lastModifiedDateTests.setEtag(testEntity4.getVersion().toString());
						lastModifiedDateTests.setContent(content);
					});
					lastModifiedDateTests = new LastModifiedDateTests();
				});
			});
		});
	}

	@Test
	public void noop() {
	}
}