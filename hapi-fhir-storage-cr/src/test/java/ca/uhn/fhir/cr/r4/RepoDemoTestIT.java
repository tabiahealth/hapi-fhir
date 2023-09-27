package ca.uhn.fhir.cr.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.cr.repo.HapiFhirRepository;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.util.BundleBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.RestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RepoDemoTestIT extends BaseCrR4TestServer {

	@Test
	public void restRepoTest() {
		var repo = new RestRepository(ourClient);
		runTest(repo);
	}

	@Test
	public void hapiRepoTest() {
		var repo = new HapiFhirRepository(myDaoRegistry, setupRequestDetails(), ourRestfulServer);
		runTest(repo);
	}
	protected void runTest(Repository repo) {

		// Load data
		repo.transaction(this.createBundle());

		// Invoke operation
		var params = new Parameters().addParameter("_type", "Coverage");
		var b = repo.invoke(
			new IdType("Patient/123"),
			"$everything",
			params,
			Bundle.class,
			// Hmm... This _ought_ not be needed for the
			// HAPI FHIR Repository
			Map.of("Content-Type", "application/json"));

		assertNotNull(b);
		assertEquals(b.getEntry().size(), 1);

		var c = b.getEntry().get(0).getResource();
		assertEquals("Coverage/789", c.getId());
	}

	protected Bundle createBundle() {

		var b = new BundleBuilder(FhirContext.forR4Cached());
		var p = new Patient();
		p.setId("123");
		b.addTransactionUpdateEntry(p);
		b.addTransactionUpdateEntry(new Encounter().setSubjectTarget(p).setId("456"));
		b.addTransactionUpdateEntry(new Coverage().setBeneficiaryTarget(p).setId("789"));

		b.setType("transaction");

		return b.getBundleTyped();
	}

	protected RequestDetails setupRequestDetails() {
		var requestDetails = new ServletRequestDetails();
		requestDetails.setServletRequest(new MockHttpServletRequest());
		requestDetails.setServer(ourRestfulServer);
		requestDetails.setFhirServerBase(ourServerBase);
		return requestDetails;
	}
}
