package com.gymback.modules.calendar.repositories;
import com.gymback.shared.repositories.BaseCRUD;
import com.gymback.modules.calendar.model.ClassTemplate;
import com.gymback.modules.calendar.interfaces.IClassTemplateRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
@Repository
public class ClassTemplateRepository extends BaseCRUD<ClassTemplate> implements IClassTemplateRepository {
  public ClassTemplateRepository(EntityManager em){super(em,ClassTemplate.class,true);}
}
