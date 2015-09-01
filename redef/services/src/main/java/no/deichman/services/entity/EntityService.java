package no.deichman.services.entity;

import com.hp.hpl.jena.rdf.model.Model;
import no.deichman.services.entity.patch.PatchParserException;

/**
 * Responsibility: TODO.
 */
public interface EntityService {
    
    void updateWork(String work);
    Model retrieveById(EntityType type, String id);
    Model retrieveWorkItemsById(String id);
    String create(EntityType type, String jsonLd);
    void delete(Model model);
    Model patch(EntityType type, String id, String ldPatchJson) throws PatchParserException;
    boolean resourceExists(String resourceUri);
}