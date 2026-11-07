import { api } from "./api";
import { ArticleCategory, ArticleDto, CreateArticleRequest } from "../types/article";

export const articleService = {
  async getArticles(categoryId?: number, search?: string): Promise<ArticleDto[]> {
    const res = await api.get<ArticleDto[]>("/articles", {
      params: { categoryId, search },
    });
    return res.data;
  },

  async getArticle(id: number): Promise<ArticleDto> {
    const res = await api.get<ArticleDto>(`/articles/${id}`);
    return res.data;
  },

  async getCategories(): Promise<ArticleCategory[]> {
    const res = await api.get<ArticleCategory[]>("/articles/categories");
    return res.data;
  },

  async createArticle(req: CreateArticleRequest): Promise<ArticleDto> {
    const res = await api.post<ArticleDto>("/articles", req);
    return res.data;
  },

  async updateArticle(id: number, req: CreateArticleRequest): Promise<ArticleDto> {
    const res = await api.put<ArticleDto>(`/articles/${id}`, req);
    return res.data;
  },

  async deleteArticle(id: number): Promise<void> {
    await api.delete(`/articles/${id}`);
  },
};
