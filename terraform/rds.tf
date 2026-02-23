resource "aws_db_subnet_group" "rds" {
  name        = "${var.cluster_name}-rds-subnet-group"
  description = "RDS subnet group for ${var.cluster_name}"
  subnet_ids  = module.vpc.private_subnets

  tags = {
    Name        = "${var.cluster_name}-rds-subnet-group"
    Environment = var.environment
  }
}

resource "aws_security_group" "rds" {
  name        = "${var.cluster_name}-rds-sg"
  description = "Security group for RDS instance"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description      = "Allow PostgreSQL traffic from EKS worker nodes"
    from_port        = 5432
    to_port          = 5432
    protocol         = "tcp"
    security_groups  = [module.eks.node_security_group_id]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name        = "${var.cluster_name}-rds-sg"
    Environment = var.environment
  }
}

resource "aws_db_instance" "postgresql" {
  identifier              = "${var.cluster_name}-db"
  allocated_storage       = 20
  storage_type            = "gp2"
  engine                  = "postgres"
  engine_version          = "15" # Using major version for better compatibility
  instance_class          = "db.t3.micro"
  db_name                 = var.db_name
  username                = var.db_username
  password                = var.db_password
  parameter_group_name    = "default.postgres15"

  db_subnet_group_name    = aws_db_subnet_group.rds.name
  vpc_security_group_ids  = [aws_security_group.rds.id]

  publicly_accessible     = false
  multi_az                = true
  backup_retention_period = 7
  skip_final_snapshot     = true # Set to true to allow destruction
  deletion_protection     = false # Set to false to allow destruction

  tags = {
    Name        = "${var.cluster_name}-db"
    Environment = var.environment
  }
}
