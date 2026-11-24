package com.lab.dao;

import com.lab.repository.PriorityRepository;
import com.lab.repository.TestTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class SampleDAO extends com.lab.repository.SampleRepository {
    public SampleDAO() {
        super();
    }

    @Autowired
    public SampleDAO(TestTypeRepository testTypeRepository, PriorityRepository priorityRepository) {
        super(testTypeRepository, priorityRepository);
    }
}
