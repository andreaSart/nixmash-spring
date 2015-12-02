package com.nixmash.springdata.solr;

import java.util.List;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nixmash.springdata.solr.model.Product;
import com.nixmash.springdata.solr.repository.custom.CustomProductRepository;

/**
 * 
 * NixMash Spring Notes: ---------------------------------------------------
 * 
 * Based on Christoph Strobl's Spring Solr Repository Example for Spring Boot
 * 
 * On GitHub: https://goo.gl/JoAYaT
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class SolrApplicationTests extends SolrContext {

	private static final String SOLR_STRING = "solr";
	private static final int PRODUCT_ID = 1000;
	private static final int INITIAL_RECORD_COUNT = 44;

	@Autowired
	SolrOperations solrOperations;

	@After
	public void tearDown() {
		Query query = new SimpleQuery(new SimpleStringCriteria("cat:test"));
		solrOperations.delete(query);
	}

	@Resource
	CustomProductRepository customProductRepository;

	@Test
	public void testCustomQueries() {

		// Named Query from named-queries.properties
		List<Product> products = customProductRepository.findByNameOrCategory(SOLR_STRING, sortByIdDesc());
		Assert.assertEquals(1, products.size());

		// Method Name Query test for findByPopularityGreaterThanEqual()
		Product product = SolrTestUtils.createProduct(PRODUCT_ID);
		customProductRepository.save(product);

		Page<Product> popularProducts = customProductRepository.findByPopularityGreaterThanEqual(10000,
				new PageRequest(0, 10));
		Assert.assertEquals(1, popularProducts.getTotalElements());
		Assert.assertEquals(Integer.toString(PRODUCT_ID), popularProducts.getContent().get(0).getId());

	}

	@Test
	public void testRetrieveAllCount() {
		Query query = new SimpleQuery(new SimpleStringCriteria("*:*"));
		Page<Product> products = solrOperations.queryForPage(query, Product.class);
		Assert.assertEquals(INITIAL_RECORD_COUNT, products.getTotalElements());
	}

	@Test
	public void testProductCRUD() {

		// create local product object
		Product product = SolrTestUtils.createProduct(PRODUCT_ID);

		// save product to Solr Index and confirm index count increased by 1
		customProductRepository.save(product);
		Assert.assertEquals(INITIAL_RECORD_COUNT + 1, customProductRepository.count());

		// find single product from Solr
		Product loaded = customProductRepository.findOne(Integer.toString(PRODUCT_ID));
		Assert.assertEquals(product.getName(), loaded.getName());

		// update product name in Solr and confirm index count not changed
		loaded.setName("changed named");
		customProductRepository.save(loaded);
		Assert.assertEquals(INITIAL_RECORD_COUNT + 1, customProductRepository.count());

		// retrieve product from Solr and confirm name change
		loaded = customProductRepository.findOne(Integer.toString(PRODUCT_ID));
		Assert.assertEquals("changed named", loaded.getName());

		// delete the test product in Solr and confirm index count equal to initial count
		customProductRepository.delete(loaded);
		Assert.assertEquals(INITIAL_RECORD_COUNT, customProductRepository.count());

	}

	private Sort sortByIdDesc() {
		return new Sort(Sort.Direction.DESC, Product.ID_FIELD);
	}

}