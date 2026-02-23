module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.0"

  cluster_name    = var.cluster_name
  cluster_version = "1.29"

  cluster_endpoint_public_access = true

  vpc_id                   = module.vpc.vpc_id
  subnet_ids               = module.vpc.private_subnets
  control_plane_subnet_ids = module.vpc.intra_subnets

  # The module handles IAM role/policy creation by default
  # It sets up: Cluster IAM Role, Node Group IAM Role, Node Group Launch Templates

  eks_managed_node_groups = {
    default = {
      min_size     = 1
      max_size     = 3
      desired_size = 2

      instance_types = ["t3.medium"]
      capacity_type  = "ON_DEMAND"

      # AmazonEKSWorkerNodePolicy, AmazonEKS_CNI_Policy, AmazonEC2ContainerRegistryReadOnly 
      # are attached by default by the AWS EKS service to managed node groups.
    }
  }

  # Add access for the current IAM entity running terraform 
  # (Requires access_entries update in newer versions of module 19.16+)
  enable_cluster_creator_admin_permissions = true

  access_entries = var.github_actions_iam_arn != "" ? {
    github_actions = {
      # Remove system:masters group as EKS Access Entries handle permissions via policy associations
      principal_arn     = var.github_actions_iam_arn

      policy_associations = {
        admin = {
          policy_arn = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"
          access_scope = {
            type       = "cluster"
          }
        }
      }
    }
  } : {}

  tags = {
    Environment = var.environment
    Terraform   = "true"
  }
}
