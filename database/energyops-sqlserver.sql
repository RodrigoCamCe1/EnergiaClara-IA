-- EnergyOps / Analytics - SQL Server demo tables and seed
-- Ejecutar sobre la base usada por EnergiaClara AI si el backend apunta a SQL Server.

IF OBJECT_ID('dbo.energy_readings', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.energy_readings (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        facility_id VARCHAR(80) NOT NULL,
        meter_id VARCHAR(80) NOT NULL,
        measured_at DATETIMEOFFSET NOT NULL,
        kwh DECIMAL(12, 3) NOT NULL,
        voltage DECIMAL(10, 3) NULL,
        power_factor DECIMAL(5, 3) NULL,
        created_at DATETIMEOFFSET NOT NULL
    );
END;

IF OBJECT_ID('dbo.energy_baselines', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.energy_baselines (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        facility_id VARCHAR(80) NOT NULL,
        meter_id VARCHAR(80) NOT NULL,
        expected_kwh DECIMAL(12, 3) NOT NULL,
        tolerance_percent DECIMAL(7, 3) NOT NULL,
        active BIT NOT NULL CONSTRAINT df_energy_baselines_active DEFAULT 1,
        created_at DATETIMEOFFSET NOT NULL,
        CONSTRAINT uq_energy_baselines_meter UNIQUE (facility_id, meter_id),
        CONSTRAINT chk_energy_baselines_expected CHECK (expected_kwh > 0),
        CONSTRAINT chk_energy_baselines_tolerance CHECK (tolerance_percent >= 0)
    );
END;

IF OBJECT_ID('dbo.energy_anomalies', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.energy_anomalies (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        reading_id UNIQUEIDENTIFIER NOT NULL,
        facility_id VARCHAR(80) NOT NULL,
        meter_id VARCHAR(80) NOT NULL,
        measured_at DATETIMEOFFSET NOT NULL,
        type VARCHAR(40) NOT NULL,
        severity VARCHAR(20) NOT NULL,
        deviation_percent DECIMAL(9, 3) NOT NULL,
        explanation VARCHAR(700) NOT NULL,
        recommendation VARCHAR(700) NOT NULL,
        estimated_cost_impact DECIMAL(12, 2) NOT NULL,
        estimated_co2_impact DECIMAL(12, 2) NOT NULL,
        created_at DATETIMEOFFSET NOT NULL,
        CONSTRAINT fk_energy_anomalies_reading FOREIGN KEY (reading_id)
            REFERENCES dbo.energy_readings(id),
        CONSTRAINT chk_energy_anomaly_type CHECK (type IN (
            'EXCESS_CONSUMPTION',
            'LOW_POWER_FACTOR',
            'VOLTAGE_OUT_OF_RANGE'
        )),
        CONSTRAINT chk_energy_anomaly_severity CHECK (severity IN (
            'LOW',
            'MEDIUM',
            'HIGH',
            'CRITICAL'
        ))
    );
END;

IF OBJECT_ID('dbo.energy_kpi_snapshots', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.energy_kpi_snapshots (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        reading_id UNIQUEIDENTIFIER NOT NULL,
        facility_id VARCHAR(80) NOT NULL,
        meter_id VARCHAR(80) NOT NULL,
        measured_at DATETIMEOFFSET NOT NULL,
        kwh DECIMAL(12, 3) NOT NULL,
        baseline_kwh DECIMAL(12, 3) NOT NULL,
        deviation_percent DECIMAL(9, 3) NOT NULL,
        anomaly_detected BIT NOT NULL,
        estimated_cost_impact DECIMAL(12, 2) NOT NULL,
        estimated_co2_impact DECIMAL(12, 2) NOT NULL,
        created_at DATETIMEOFFSET NOT NULL,
        CONSTRAINT fk_energy_kpi_snapshots_reading FOREIGN KEY (reading_id)
            REFERENCES dbo.energy_readings(id)
    );
END;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.energy_baselines
    WHERE facility_id = 'FAC-001'
      AND meter_id = 'M-001'
)
BEGIN
    INSERT INTO dbo.energy_baselines (
        id,
        facility_id,
        meter_id,
        expected_kwh,
        tolerance_percent,
        active,
        created_at
    ) VALUES (
        NEWID(),
        'FAC-001',
        'M-001',
        100.000,
        15.000,
        1,
        SYSUTCDATETIME()
    );
END;
ELSE
BEGIN
    UPDATE dbo.energy_baselines
    SET expected_kwh = 100.000,
        tolerance_percent = 15.000,
        active = 1
    WHERE facility_id = 'FAC-001'
      AND meter_id = 'M-001';
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_energy_readings_meter_time'
      AND object_id = OBJECT_ID('dbo.energy_readings')
)
BEGIN
    CREATE INDEX idx_energy_readings_meter_time
        ON dbo.energy_readings(facility_id, meter_id, measured_at DESC);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_energy_anomalies_time'
      AND object_id = OBJECT_ID('dbo.energy_anomalies')
)
BEGIN
    CREATE INDEX idx_energy_anomalies_time
        ON dbo.energy_anomalies(measured_at DESC);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_energy_kpi_snapshots_time'
      AND object_id = OBJECT_ID('dbo.energy_kpi_snapshots')
)
BEGIN
    CREATE INDEX idx_energy_kpi_snapshots_time
        ON dbo.energy_kpi_snapshots(measured_at DESC);
END;
