export interface ArticleCategory {
  id: number;
  name: string;
}

export interface ArticleDto {
  id: number;
  title: string;
  body: string;
  summary: string;
  categoryId: number | null;
  categoryName: string;
  authorId: number;
  authorName: string;
  published: boolean;
  publishedAt: string;
}

export interface CreateArticleRequest {
  title: string;
  body: string;
  categoryId?: number;
  categoryName?: string;
  published?: boolean;
}
