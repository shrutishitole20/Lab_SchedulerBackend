package com.lab.dao;

import com.lab.repository.TestTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class MachineDAO extends com.lab.repository.MachineRepository {
    public MachineDAO() {
        super();
    }

    @Autowired
    public MachineDAO(TestTypeRepository testTypeRepository) {
        super(testTypeRepository);
    }
}
